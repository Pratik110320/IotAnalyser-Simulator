package com.pratik.IotAnalyser.simulator;

import com.pratik.IotAnalyser.dtos.sensorDto.SensorRegistrationDto;
import com.pratik.IotAnalyser.model.Device;
import com.pratik.IotAnalyser.model.SensorData;
import com.pratik.IotAnalyser.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Multi-device simulator that is resilient to slight DTO/model differences.
 * - Uses reflection fallbacks when DTO constructors or setters are missing.
 * - Generates random readings locally instead of relying on SensorData.random(...)
 */
@Service
public class MultiDeviceSimulatorService {

    private static final Logger log = LoggerFactory.getLogger(MultiDeviceSimulatorService.class);

    private final SimulatorConfig config;
    private final WebSocketClientService clientService;
    private final ThreadPoolTaskScheduler scheduler;

    private final Map<Long, StompSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    private final DeviceRepository deviceRepository;
    public MultiDeviceSimulatorService(SimulatorConfig config, WebSocketClientService clientService, DeviceRepository deviceRepository) {
        this.config = config;
        this.clientService = clientService;
        this.deviceRepository = deviceRepository;

        this.scheduler = new ThreadPoolTaskScheduler();
        this.scheduler.setPoolSize(Math.max(2, Runtime.getRuntime().availableProcessors()));
        this.scheduler.initialize();
    }

    /** ---------------- Simulation Control ---------------- **/
    public void startSimulation() {
        log.info("Starting simulation for {} devices", config.getDeviceCount());
        for (long i = 1; i <= config.getDeviceCount(); i++) {
            registerOneDevice(i);
        }
    }

    public void stopSimulation() {
        log.info("Stopping simulation");
        scheduledTasks.values().forEach(f -> f.cancel(true));
        activeSessions.values().forEach(session -> {
            try { session.disconnect(); } catch (Exception ignored) {}
        });
        scheduledTasks.clear();
        activeSessions.clear();
    }

    public void registerOneDevice(Long deviceIndex) {
        log.info("Registering device {}", deviceIndex);

        // 1. Create & persist Device
        Device device = new Device();
        device.setDeviceName("DEVICE_" + deviceIndex);
        device.setDeviceType("SIMULATED");
        device.setRegisteredAt(LocalDateTime.now());
        device.setLastActiveAt(LocalDateTime.now());
        device.setStatus(Device.Status.ONLINE);

        Device persistedDevice = deviceRepository.save(device);
        final Long persistedId = persistedDevice.getDeviceId();
        final Device finalDevice = persistedDevice;  // final reference for lambda

        log.info("Device persisted with ID {}", persistedId);

        // 2. Connect WebSocket
        StompSession session = clientService.connect(config.getServerUrl());

        if (session != null && session.isConnected()) {
            activeSessions.put(persistedId, session);

            // 3. Send registration DTO
            SensorRegistrationDto dto = buildRegistrationDto(
                    persistedId,
                    finalDevice.getDeviceName(),
                    config.getSensorTypes()
            );
            clientService.registerDevice(session, "/app/register", dto);

            // 4. Schedule periodic sensor data
            ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {

                for (String sensorType : config.getSensorTypes()) {
                    // send 2 readings at once to look busy
                    for (int i = 0; i < 2; i++) {
                        SensorData data = createRandomSensorData(persistedId, sensorType);
                        clientService.sendSensorData(session, "/app/sensor-data", data);
                    }
                }
            }, config.getDataPushInterval());

            scheduledTasks.put(persistedId, task);

        } else {
            log.warn("Could not create stomp session for device {} — not scheduling tasks", deviceIndex);
        }
    }

    public void disconnectDevice(Long deviceId) {
        log.info("Disconnecting device {}", deviceId);
        Optional.ofNullable(activeSessions.remove(deviceId)).ifPresent(session -> {
            try { session.disconnect(); } catch (Exception ignored) {}
        });
        Optional.ofNullable(scheduledTasks.remove(deviceId)).ifPresent(f -> f.cancel(true));
    }

    public void reconnectDevice(Long deviceId) {
        log.info("Reconnecting device {}", deviceId);
        disconnectDevice(deviceId);
        registerOneDevice(deviceId);
    }

    /** ---------------- Anomaly Injection ---------------- **/
    public void injectAnomalyToDevice(Long deviceId) {
        log.info("Injecting anomaly for device {}", deviceId);
        StompSession session = activeSessions.get(deviceId);
        if (session != null && session.isConnected()) {
            // pick a sensor type and force anomaly
            String sensorType = config.getSensorTypes().isEmpty() ? "SENSOR" : config.getSensorTypes().get(0);
            SensorData anomaly = createAnomalySensorData(deviceId, sensorType, true);
            clientService.sendSensorData(session, "/app/sensor-data", anomaly);
        } else {
            log.warn("Cannot inject anomaly — session missing or disconnected for device {}", deviceId);
        }
    }

    /** ---------------- Utils: DTO & Model builders ---------------- **/

    /**
     * Try to construct a SensorRegistrationDto using:
     * 1) a (Long,String,List) constructor if present
     * 2) no-arg constructor + public setters (setDeviceId, setDeviceName, setSensorTypes)
     * 3) field access fallback (reflectively set fields)
     */
    private SensorRegistrationDto buildRegistrationDto(Long deviceId, String deviceName, List<String> sensorTypes) {
        try {
            // try specific constructor
            Constructor<SensorRegistrationDto> ctor = SensorRegistrationDto.class.getConstructor(Long.class, String.class, List.class);
            return ctor.newInstance(deviceId, deviceName, sensorTypes);
        } catch (NoSuchMethodException ignored) {
            // fallback to no-arg + setters / fields
        } catch (Exception e) {
            log.warn("Failed to construct SensorRegistrationDto with (Long,String,List) ctor: {}", e.getMessage());
        }

        try {
            SensorRegistrationDto dto = SensorRegistrationDto.class.getDeclaredConstructor().newInstance();

            // try common setters
            tryInvokeSetter(dto, "setDeviceId", deviceId);
            tryInvokeSetter(dto, "setDeviceName", deviceName);
            tryInvokeSetter(dto, "setSensorTypes", sensorTypes);

            // If setters didn't exist, try field names directly
            trySetFieldIfPresent(dto, "deviceId", deviceId);
            trySetFieldIfPresent(dto, "deviceName", deviceName);
            trySetFieldIfPresent(dto, "sensorTypes", sensorTypes);

            return dto;
        } catch (Exception ex) {
            log.error("Unable to create SensorRegistrationDto reflectively: {}", ex.getMessage(), ex);
            // As last resort, return null — callers should handle null (you can also throw)
            return null;
        }
    }
    private SensorData createRandomSensorData(Long deviceId, String sensorType) {
        SensorData sd = new SensorData();

        // ✅ create stub Device with only ID
        Device d = new Device(deviceId);
        d.setDeviceId(deviceId);
        sd.setDevice(d);

        // ✅ set sensor info
        sd.setSensorType(sensorType);
        sd.setTimestamp(LocalDateTime.now());

        double value = ThreadLocalRandom.current().nextDouble(10.0, 45.0);
        sd.setValue(value);

        boolean anomaly = ThreadLocalRandom.current().nextDouble() < config.getAnomalyProbability();
        sd.setAnomaly(anomaly);

        return sd;
    }

    private SensorData createAnomalySensorData(Long deviceId, String sensorType, boolean forceAnomaly) {
        SensorData sd = createRandomSensorData(deviceId, sensorType);

        sd.setAnomaly(true); // force anomaly
        sd.setValue(sd.getValue() * 1.5); // exaggerate value

        return sd;
    }


    /** ---------------- Reflection helpers ---------------- **/

    private void tryInvokeSetter(Object target, String setterName, Object value) {
        if (target == null || setterName == null) return;
        try {
            Method m = findMethodIgnoreCase(target.getClass(), setterName, value == null ? new Class<?>[]{Object.class} : new Class<?>[]{value.getClass()});
            if (m != null) {
                m.setAccessible(true);
                m.invoke(target, value);
            }
        } catch (Exception ignored) {
            // ignore — fallback will try fields
        }
    }

    private void trySetFieldIfPresent(Object target, String fieldName, Object value) {
        if (target == null) return;
        try {
            Field f = findFieldIgnoreCase(target.getClass(), fieldName);
            if (f != null) {
                f.setAccessible(true);
                f.set(target, value);
            }
        } catch (Exception ignored) {}
    }

    private void bumpNumericField(Object target, String fieldName, double multiplier) {
        Field f = findFieldIgnoreCase(target.getClass(), fieldName);
        if (f == null) return;
        f.setAccessible(true);
        Object val;
        try {
            val = f.get(target);
            if (val instanceof Number) {
                double newVal = ((Number) val).doubleValue() * multiplier;
                // set back using appropriate type
                if (val instanceof Integer) f.set(target, (int) newVal);
                else if (val instanceof Long) f.set(target, (long) newVal);
                else f.set(target, newVal);
            }
        } catch (Exception ignored) {}
    }

    private Method findMethodIgnoreCase(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
        // try exact match first
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException ignored) {}

        // try any method with same name (ignore case) and parameter count 1
        for (Method m : clazz.getMethods()) {
            if (m.getName().equalsIgnoreCase(methodName) && m.getParameterCount() == 1) {
                return m;
            }
        }
        return null;
    }

    private Field findFieldIgnoreCase(Class<?> clazz, String fieldName) {
        Class<?> c = clazz;
        while (c != null) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getName().equalsIgnoreCase(fieldName)) return f;
            }
            c = c.getSuperclass();
        }
        return null;
    }

    public void waitForAnalyser() {
        try {
            Thread.sleep(3000L); // 3 seconds — increase if necessary
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
}}

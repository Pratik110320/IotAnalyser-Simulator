// pratik110320/iotanalyser-simulator/IotAnalyser-Simulator-ba46f81a8355039545da0025050cefdd674b83a0/src/main/java/com/pratik/IotAnalyser/simulator/MultiDeviceSimulatorService.java
package com.pratik.IotAnalyser.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MultiDeviceSimulatorService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // deviceId -> scheduled task
    private final Map<String, ScheduledFuture<?>> deviceTasks = new ConcurrentHashMap<>();
    private volatile boolean simulatorRunning = false;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public MultiDeviceSimulatorService() {
        scheduler.setPoolSize(4);
        scheduler.initialize();
    }


    public void waitForAnalyser() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            System.out.println("waitForAnalyser interrupted");
        } }
    /**
     * Auto-start simulator on boot (keeps the auto-start behavior).
     * We schedule a small delayed start so websocket endpoints have time to be ready.
     */
    @PostConstruct
    public void autoStartOnBoot() {
        // schedule a one-time task 2s after boot to start simulator (idempotent)
        scheduler.schedule(this::startSimulationIfNeeded, new Date(System.currentTimeMillis() + 2000));
    }

    /**
     * Handle frontend connection notifications from SensorWebSocketHandler (if you use that).
     */
    public synchronized void handleFrontendMessage(String msg) {
        if ("FRONTEND_CONNECTED".equals(msg)) {
            startSimulationIfNeeded();
            simulatorRunning = true; // ensure flag is set
            notifyStatus();
        } else if ("FRONTEND_DISCONNECTED".equals(msg)) {
            stopSimulation();
            notifyStatus();
        }
    }

    /**
     * Start simulation if not already running
     */
    public synchronized void startSimulationIfNeeded() {
        if (simulatorRunning) return;
        simulatorRunning = true;
        System.out.println("Simulator started");

        // Example: simulate 3 devices (adjust as required)
        startDevice("device-1");
        startDevice("device-2");
        startDevice("device-3");

        notifyStatus();
    }

    /**
     * Start a single device simulator (idempotent)
     */
    public synchronized void startDevice(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) return;
        if (deviceTasks.containsKey(deviceId)) return;

        // schedule periodic pushes (every 2 seconds)
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> pushData(deviceId), 2000);
        deviceTasks.put(deviceId, task);

        // ensure overall running flag is true
        simulatorRunning = true;
        notifyStatus();
    }

    /**
     * Stop simulation (all devices)
     */
    public synchronized void stopSimulation() {
        if (!simulatorRunning && deviceTasks.isEmpty()) {
            System.out.println("Simulator already stopped");
            return;
        }

        simulatorRunning = false;
        deviceTasks.values().forEach(task -> task.cancel(true));
        deviceTasks.clear();

        System.out.println("Simulator stopped");
        notifyStatus();
    }

    /**
     * Stop a single device
     * @return true if a running task was cancelled
     */
    public synchronized boolean stopDevice(String deviceId) {
        if (deviceId == null) return false;
        ScheduledFuture<?> f = deviceTasks.remove(deviceId);
        if (f != null) {
            f.cancel(true);
            // if no devices left, mark simulator not running
            if (deviceTasks.isEmpty()) simulatorRunning = false;
            notifyStatus();
            return true;
        }
        return false;
    }

    /**
     * Get running device ids
     */
    public Set<String> getRunningDeviceIds() {
        return Collections.unmodifiableSet(deviceTasks.keySet());
    }

    public boolean isRunning() {
        return simulatorRunning;
    }

    /**
     * Push simulated data for a device.
     * Sends a Map (not a JSON string) so messagingTemplate serializes to JSON correctly.
     */
    private void pushData(String deviceId) {
        if (!simulatorRunning) return; // safety check (also helps when single device stopped)

        try {
            // pick a sensor type for this tick (random or deterministic per device)
            String sensorType = pickSensorTypeFor(deviceId);
            double value = nextValueFor(sensorType, deviceId);
            boolean anomaly = Math.random() < 0.2; // 20% chance of anomaly


            Map<String, Object> data = new HashMap<>();
            data.put("deviceId", deviceId);
            data.put("sensorType", sensorType);
            data.put("value", Math.round(value * 100.0) / 100.0); // round to 2 decimals
            data.put("timestamp", Instant.now().toString());      // ISO timestamp
            data.put("time", LocalTime.now().format(TIME_FMT));   // HH:mm:ss for display if needed
            data.put("anomaly", anomaly);
            // send the Map object directly — Spring's message converters will turn it into JSON
            messagingTemplate.convertAndSend("/topic/sensor-data", data);

            // debug log (optional)
            System.out.println("Simulated data: " + objectMapper.writeValueAsString(data));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Notify frontends about simulator state changes over a control topic.
     * Frontend should subscribe to /topic/simulator-control to receive these.
     */
    private void notifyStatus() {
        try {
            Map<String, Object> status = Map.of(
                    "isRunning", simulatorRunning,
                    "runningDeviceIds", getRunningDeviceIds(),
                    "timestamp", Instant.now().toString()
            );
            messagingTemplate.convertAndSend("/topic/simulator-control", status);
        } catch (Exception e) {
            // log and move on
            System.err.println("Failed to notify simulator status: " + e.getMessage());
        }
    }

    /**
     * Simple deterministic sensor type picker — modifies per deviceId so you get mixed sensor types.
     */
    private String pickSensorTypeFor(String deviceId) {
        int v = Math.abs(Objects.hashCode(deviceId)) % 3;
        return switch (v) {
            case 0 -> "TEMPERATURE";
            case 1 -> "HUMIDITY";
            default -> "MOTION";
        };
    }

    /**
     * Produce a realistic-ish numeric reading based on sensor type and device id
     */
    private double nextValueFor(String sensorType, String deviceId) {
        int baseSeed = Math.abs(deviceId.hashCode()) % 50;
        return switch (sensorType) {
            case "TEMPERATURE" -> 15.0 + baseSeed * 0.2 + ThreadLocalRandom.current().nextDouble(-3.0, 3.0);
            case "HUMIDITY" -> 30.0 + baseSeed * 0.4 + ThreadLocalRandom.current().nextDouble(-5.0, 5.0);
            default -> ThreadLocalRandom.current().nextDouble(0.0, 100.0); // MOTION or other sensors
        };
    }
}
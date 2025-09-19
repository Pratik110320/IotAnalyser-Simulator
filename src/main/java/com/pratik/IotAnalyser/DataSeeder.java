package com.pratik.IotAnalyser;


import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.pratik.IotAnalyser.repository.DeviceRepository;
import com.pratik.IotAnalyser.model.Device;

@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private final DeviceRepository deviceRepository;

    public DataSeeder(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (deviceRepository.count() == 0) { // Only seed if the database is empty
            Device device1 = new Device("device-1", "MOTION_SENSOR", Device.Status.ONLINE);
            Device device2 = new Device("device-2", "TEMPERATURE_SENSOR", Device.Status.ONLINE);
            Device device3 = new Device("device-3", "HUMIDITY_SENSOR", Device.Status.ONLINE);

            deviceRepository.save(device1);
            deviceRepository.save(device2);
            deviceRepository.save(device3);
        }
    }
}
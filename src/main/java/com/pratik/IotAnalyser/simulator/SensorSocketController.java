package com.pratik.IotAnalyser.simulator;



import com.pratik.IotAnalyser.dtos.sensorDto.SensorRegistrationDto;
import com.pratik.IotAnalyser.dtos.sensorDto.SensorResponseDto;
import com.pratik.IotAnalyser.model.SensorData;
import com.pratik.IotAnalyser.service.SensorDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class SensorSocketController {

    @Autowired
    private SimpMessagingTemplate template;
    private static final Logger log = LoggerFactory.getLogger(MultiDeviceSimulatorService.class);

    @Autowired
    private SensorDataService sensorDataService;

    // Simulator sends device registration
    @MessageMapping("/register")
    public void registerDevice(SensorRegistrationDto dto) {
        // broadcast to subscribers
        template.convertAndSend("/topic/devices", dto);
    }

    // Simulator sends sensor data
    @MessageMapping("/sensor-data")
    public void handleSensorData(SensorData data) {
        // persist into DB
        log.info("WS received: {}", data);

        SensorResponseDto saved = sensorDataService.addSensorDataFromSocket(data);
        // broadcast to frontend
        template.convertAndSend("/topic/sensor-data", saved);
    }
}


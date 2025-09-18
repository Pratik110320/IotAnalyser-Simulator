package com.pratik.IotAnalyser.service;

import com.pratik.IotAnalyser.model.SensorData;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

    private final SimpMessagingTemplate messagingTemplate;

    public AlertService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void checkForAlerts(SensorData sensorData) {
        if ("TEMPERATURE".equals(sensorData.getSensorType()) && sensorData.getValue() > 30.0) {
            String message = String.format("High temperature alert for device %d: %.2f°C",
                    sensorData.getDevice().getDeviceId(), sensorData.getValue());
            messagingTemplate.convertAndSend("/topic/alerts", message);
        }
    }
}
package com.pratik.IotAnalyser.service;

import com.pratik.IotAnalyser.dtos.sensorDto.*;
import com.pratik.IotAnalyser.model.Device;
import com.pratik.IotAnalyser.model.SensorData;
import com.pratik.IotAnalyser.exception.ResourceNotFoundException;
import com.pratik.IotAnalyser.repository.DeviceRepository;
import com.pratik.IotAnalyser.repository.SensorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SensorDataService {

    private final SensorRepository sensorRepository;
    private final SensorMapper sensorMapper;
    private final DeviceRepository deviceRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    public SensorDataService(SensorRepository sensorRepository, SensorMapper sensorMapper, DeviceRepository deviceRepository) {
        this.sensorRepository = sensorRepository;
        this.sensorMapper = sensorMapper;
        this.deviceRepository = deviceRepository;
    }


    @Transactional
    public SensorResponseDto addSensorData(SensorRegistrationDto sensorRegistrationDto) {
        if (sensorRegistrationDto == null) {
            throw new IllegalArgumentException("Sensor Registration Data can't be null.");
        }

        // Map incoming DTO → Entity
        SensorData sensorData = sensorMapper.toEntity(sensorRegistrationDto);
        sensorData.setTimestamp(LocalDateTime.now());

        // 🔑 Ensure device is set from DB
        if (sensorRegistrationDto.getDeviceId() != null) {
            Device device = deviceRepository.findById(sensorRegistrationDto.getDeviceId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Device not found with id " + sensorRegistrationDto.getDeviceId()));
            sensorData.setDevice(device);
        } else {
            throw new IllegalArgumentException("Device ID is required.");
        }

        // Save sensor data
        SensorData savedSensor = sensorRepository.save(sensorData);

        // Broadcast saved data to WebSocket subscribers
        SensorResponseDto broadcastDto = sensorMapper.toResponseDto(savedSensor);
        messagingTemplate.convertAndSend("/topic/sensorData", broadcastDto);

        return broadcastDto;
    }
    public List<SensorSummaryDto> getSensorData() {
        List<SensorData> sensorData = sensorRepository.findAll();
        return sensorData.stream()
                .map(sensorMapper::toSummaryDto)
                .collect(Collectors.toList()); // will be empty if no DB rows
    }



    public SensorResponseDto getSensorDataDetails(Long id) {
        SensorData sensorData = sensorRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("No Sensor Data Found."));
        return sensorMapper.toResponseDto(sensorData);
    }


    @Transactional
    public SensorResponseDto updateSensorData(Long id, SensorUpdateDto sensorUpdateDto) {
        SensorData existingData = sensorRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("No Sensor Data Found."));
        sensorMapper.updateSensorDataFromDto(sensorUpdateDto,existingData);
        SensorData savedData = sensorRepository.save(existingData);
        if (savedData == null){
            throw new IllegalArgumentException("Sensor Data Could Not Be Updated.");
        }
        return sensorMapper.toResponseDto(savedData);
    }


    public List<SensorSummaryDto> getSensorDataByTypes(String sensorType) {
        List<SensorData> sensorData = sensorRepository.findSensorDataByType(sensorType);
        if(sensorData.isEmpty()){
            throw new ResourceNotFoundException("No Sensor Data Found.");
        }

        return  sensorData.stream()
                .map(sensorMapper::toSummaryDto)
                .collect(Collectors.toList());
    }

    public List<SensorResponseDto> getSensorDataByAnomalies(Boolean anomaly) {

        if (!Boolean.TRUE.equals(anomaly)) {
            throw new IllegalArgumentException("This endpoint only supports anomaly = true");
        }
        List<SensorData> sensorData = sensorRepository.findSensorDataByAnomaly(anomaly);
        if (sensorData.isEmpty()) {
            throw new ResourceNotFoundException("No sensor data found with anomaly = true");
        }
        return sensorData.stream()
                .map(sensorMapper::toResponseDto)
                .collect(Collectors.toList());
    }
    public SensorResponseDto addSensorDataFromSocket(SensorData data) {
        SensorData saved = sensorRepository.save(data);
        return sensorMapper.toResponseDto(saved);
    }


    @Transactional
    public void deleteSensorData(Long id) {
        if (!sensorRepository.existsById(id)) {
            throw new RuntimeException("Sensor data with id " + id + " not found");
        }
        sensorRepository.deleteById(id);
    }
}

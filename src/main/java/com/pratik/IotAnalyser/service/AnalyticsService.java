package com.pratik.IotAnalyser.service;

import com.pratik.IotAnalyser.model.SensorData;
import com.pratik.IotAnalyser.repository.SensorRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final SensorRepository sensorRepository;

    public AnalyticsService(SensorRepository sensorRepository) {
        this.sensorRepository = sensorRepository;
    }

    public Map<String, DoubleSummaryStatistics> getDailyStatistics(LocalDateTime date) {
        LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<SensorData> sensorData = sensorRepository.findAll().stream()
                .filter(data -> !data.getTimestamp().isBefore(startOfDay) && data.getTimestamp().isBefore(endOfDay))
                .collect(Collectors.toList());

        return sensorData.stream()
                .collect(Collectors.groupingBy(
                        SensorData::getSensorType,
                        Collectors.summarizingDouble(SensorData::getValue)
                ));
    }
}
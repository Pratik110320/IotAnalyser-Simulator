package com.pratik.IotAnalyser.service;

import com.pratik.IotAnalyser.dtos.sensorDto.SensorRegistrationDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherDataCollectorServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SensorDataService sensorDataService;

    @InjectMocks
    private WeatherDataCollectorService weatherDataCollectorService;

    @Test
    void fetchWeatherData_ShouldProcessAndSaveSensorData() {
        // 1. Setup: Mock the API response
        Map<String, Object> mainData = Map.of("temp", 25.5, "humidity", 60.0);
        Map<String, Object> mockResponse = Map.of("main", mainData);

        // When the RestTemplate is called, return our mock response
        when(restTemplate.getForObject(any(String.class), eq(Map.class))).thenReturn(mockResponse);

        // 2. Execute: Call the method we want to test
        weatherDataCollectorService.fetchWeatherData();

        // 3. Verify: Check if our service behaved as expected
        ArgumentCaptor<SensorRegistrationDto> captor = ArgumentCaptor.forClass(SensorRegistrationDto.class);

        // Verify that addSensorData was called twice (once for temp, once for humidity)
        verify(sensorDataService, times(2)).addSensorData(captor.capture());

        List<SensorRegistrationDto> capturedDtos = captor.getAllValues();

        // Assertions for Temperature
        SensorRegistrationDto tempDto = capturedDtos.get(0);
        assertEquals(1L, tempDto.getDeviceId());
        assertEquals(25.5, tempDto.getValue());
        assertEquals("TEMPERATURE", tempDto.getSensorType());
        assertEquals("°C", tempDto.getUnit());

        // Assertions for Humidity
        SensorRegistrationDto humidityDto = capturedDtos.get(1);
        assertEquals(1L, humidityDto.getDeviceId());
        assertEquals(60.0, humidityDto.getValue());
        assertEquals("HUMIDITY", humidityDto.getSensorType());
        assertEquals("%", humidityDto.getUnit());
    }
}
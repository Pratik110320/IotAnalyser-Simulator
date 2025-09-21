// src/main/java/com/pratik/IotAnalyser/service/WeatherDataCollectorService.java
package com.pratik.IotAnalyser.service;

import com.pratik.IotAnalyser.dtos.sensorDto.SensorRegistrationDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Service
public class WeatherDataCollectorService {

    private static final Logger log = LoggerFactory.getLogger(WeatherDataCollectorService.class);

    private final SensorDataService sensorDataService;
    private final RestTemplate restTemplate;

    // This now reads from an environment variable named OPENWEATHERMAP_API_KEY
    @Value("${openweathermap.api.key}")
    private String apiKey;

    public WeatherDataCollectorService(SensorDataService sensorDataService, RestTemplate restTemplate) {
        this.sensorDataService = sensorDataService;
        this.restTemplate = restTemplate;
    }

    @Scheduled(fixedRate = 6000) // Every 10 minutes
    public void fetchWeatherData() {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("84b2221fb299c9649673758febaed6f8")) {
            log.warn("OpenWeatherMap API key is not configured. Skipping weather data fetch.");
            return;
        }

        String url = "https://api.openweathermap.org/data/2.5/weather?q=Nagpur&appid=" + apiKey + "&units=metric";
        try {
            var weatherData = restTemplate.getForObject(url, Map.class);

            if (weatherData != null && weatherData.containsKey("main")) {
                Map<String, Number> main = (Map<String, Number>) weatherData.get("main");
                double temp = main.get("temp").doubleValue();
                double humidity = main.get("humidity").doubleValue();

                // Save to your sensor service
                sensorDataService.addSensorData(new SensorRegistrationDto(1L, temp, "TEMPERATURE", "°C", true));
                sensorDataService.addSensorData(new SensorRegistrationDto(1L, humidity, "HUMIDITY", "%", true));

                log.info("Weather data fetched successfully: Temp={}°C, Humidity={}%", temp, humidity);
            } else {
                log.warn("Weather data response was null or missing 'main' field");
            }

        } catch (Exception e) {
            // IMPROVED LOGGING: This will now print the full error details to your Render logs
            log.error("Error while fetching weather data from OpenWeatherMap API. Full error:", e);
        }
    }
}

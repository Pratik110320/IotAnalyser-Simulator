package com.pratik.IotAnalyser.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Value("${simulator.server-url}")
    private String simulatorServerUrl;

    @GetMapping("/ws-url")
    public Map<String, String> getWsUrl() {
        return Map.of("wsUrl", simulatorServerUrl);
    }
}

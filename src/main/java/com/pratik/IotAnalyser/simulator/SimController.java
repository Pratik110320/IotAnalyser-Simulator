package com.pratik.IotAnalyser.simulator;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/simulator")
public class SimController {

    private final MultiDeviceSimulatorService simulator;
    private final SimpMessagingTemplate messagingTemplate;
    public SimController(MultiDeviceSimulatorService simulator, SimpMessagingTemplate messagingTemplate) {
        this.simulator = simulator;
        this.messagingTemplate = messagingTemplate;
    }
    public void notifySimulator(String command) {
        messagingTemplate.convertAndSend("/topic/simulator-control", command);
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        Set<String> running = simulator.getRunningDeviceIds();
        return ResponseEntity.ok(Map.of(
                "isRunning", simulator.isRunning(),
                "runningDeviceIds", running
        ));
    }

    @PostMapping("/startAll")
    public ResponseEntity<?> startAll() {
        simulator.startSimulationIfNeeded();
        return ResponseEntity.ok(Map.of("status", "startedAll"));
    }

    @PostMapping("/stopAll")
    public ResponseEntity<?> stopAll() {
        simulator.stopSimulation();
        return ResponseEntity.ok(Map.of("status", "stoppedAll"));
    }

    @PostMapping("/start/{deviceId}")
    public ResponseEntity<?> startDevice(@PathVariable String deviceId) {
        simulator.startDevice(deviceId);
        return ResponseEntity.ok(Map.of("status", "started", "deviceId", deviceId));
    }

    @PostMapping("/stop/{deviceId}")
    public ResponseEntity<?> stopDevice(@PathVariable String deviceId) {
        boolean wasRunning = simulator.stopDevice(deviceId);
        return ResponseEntity.ok(Map.of("status", wasRunning ? "stopped" : "wasNotRunning", "deviceId", deviceId));
    }
}

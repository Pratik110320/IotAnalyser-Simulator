package com.pratik.IotAnalyser.simulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "simulator", name = "enabled", havingValue = "true")
@DependsOn("dataSeeder")
public class SimulatorRunner {

    private static final Logger log = LoggerFactory.getLogger(SimulatorRunner.class);

    private final MultiDeviceSimulatorService simulatorService;

    public SimulatorRunner(MultiDeviceSimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("ApplicationReadyEvent received. Preparing simulator...");

        try {
            // Small delay to allow analyser/websocket endpoints to bind
            Thread.sleep(800);

            // Ensure analyser is ready (simple wait)
            simulatorService.waitForAnalyser();

            log.info("SimulatorRunner initialized successfully.");
            log.info("Simulation will auto-start ONLY when frontend connects (via SimulatorLifecycle).");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("SimulatorRunner initialization interrupted");
        } catch (Throwable t) {
            log.error("SimulatorRunner failed to initialize: {}", t.getMessage(), t);
        }
    }

    @EventListener(ContextClosedEvent.class)
    public void onContextClosed() {
        log.info("ContextClosedEvent received. Ensuring simulator is stopped...");
        try {
            simulatorService.stopSimulation();
        } catch (Throwable t) {
            log.warn("Error while stopping simulator: {}", t.getMessage());
        }
    }
}

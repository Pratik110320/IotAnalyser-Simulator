package com.pratik.IotAnalyser.simulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Starts the MultiDeviceSimulatorService after the application is fully ready.
 * Runs the simulator on a separate thread so startup isn't blocked.
 *
 * Only active when simulator.enabled=true in properties.
 */
@Component
@ConditionalOnProperty(prefix = "simulator", name = "enabled", havingValue = "true")
public class SimulatorRunner {

    private static final Logger log = LoggerFactory.getLogger(SimulatorRunner.class);

    private final MultiDeviceSimulatorService simulatorService;
    private final SimulatorConfig simulatorConfig;
    private volatile Thread runnerThread;

    public SimulatorRunner(MultiDeviceSimulatorService simulatorService, SimulatorConfig simulatorConfig) {
        this.simulatorService = simulatorService;
        this.simulatorConfig = simulatorConfig;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("ApplicationReadyEvent received. Starting simulator (if enabled) ...");

        // Start simulator on a background thread so we don't block the Spring startup.
        runnerThread = new Thread(() -> {
            try {
                // give analyser/websocket endpoints a chance to bind; your service also has waitForAnalyser()
                try {
                    Thread.sleep(800); // small safety wait (adjust if needed)
                } catch (InterruptedException ignored) {}

                // allow service-level wait (3s) to run if you kept waitForAnalyser()
                try {
                    simulatorService.waitForAnalyser();
                } catch (Throwable t) {
                    log.warn("simulatorService.waitForAnalyser() threw: {}", t.getMessage());
                }

                log.info("Starting MultiDeviceSimulatorService with deviceCount={}", simulatorConfig.getDeviceCount());
                simulatorService.startSimulation();
            } catch (Throwable t) {
                log.error("Simulator failed to start: {}", t.getMessage(), t);
            }
        }, "simulator-runner-thread");

        runnerThread.setDaemon(true);
        runnerThread.start();
    }

    /**
     * Stop simulator when application context is closing.
     */
    @EventListener(ContextClosedEvent.class)
    public void onContextClosed() {
        log.info("ContextClosedEvent received. Stopping simulator...");
        try {
            simulatorService.stopSimulation();
        } catch (Throwable t) {
            log.warn("Error while stopping simulator: {}", t.getMessage());
        }

        // interrupt the runner thread if it's still alive
        try {
            if (runnerThread != null && runnerThread.isAlive()) {
                runnerThread.interrupt();
            }
        } catch (Exception ignored) {}
    }
}

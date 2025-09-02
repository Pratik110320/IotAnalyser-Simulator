package com.pratik.IotAnalyser.simulator;

import com.pratik.IotAnalyser.webSocket.WebSocketSessionTracker;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Service
public class SimulatorLifecycle {

    private final MultiDeviceSimulatorService simulatorService;
    private final WebSocketSessionTracker tracker;

    public SimulatorLifecycle(MultiDeviceSimulatorService simulatorService,
                              WebSocketSessionTracker tracker) {
        this.simulatorService = simulatorService;
        this.tracker = tracker;
    }

    @EventListener
    public void onConnect(SessionConnectEvent event) {
        tracker.addSession();
        if (tracker.getActiveSessions() == 1) {
            // Instead of forcing start here, just let frontend decide
            simulatorService.startSimulationIfNeeded();
            System.out.println("SimulatorLifecycle: frontend connected → simulator auto-started");
        }
    }


    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        tracker.removeSession();
        if (!tracker.hasActiveSessions()) {
            // last client disconnected → stop simulation
            simulatorService.stopSimulation();
        }
    }
}

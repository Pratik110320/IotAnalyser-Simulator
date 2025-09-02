package com.pratik.IotAnalyser.simulator;

import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class FrontendConnectionListener implements ApplicationListener<SessionConnectEvent> {

    private final SimController simController;

    public FrontendConnectionListener(SimController simController) {
        this.simController = simController;
    }

    @Override
    public void onApplicationEvent(SessionConnectEvent event) {
        // When frontend connects
        simController.notifySimulator("START_SIMULATION");
    }
}


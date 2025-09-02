package com.pratik.IotAnalyser.simulator;


import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class FrontendDisconnectListener implements ApplicationListener<SessionDisconnectEvent> {

    private final SimController simController;

    public FrontendDisconnectListener(SimController simController) {
        this.simController = simController;
    }

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        // When frontend disconnects
        simController.notifySimulator("STOP_SIMULATION");
    }
}
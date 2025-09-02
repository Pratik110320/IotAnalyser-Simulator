package com.pratik.IotAnalyser.simulator;



import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;

@Component
public class SensorWebSocketHandler extends TextWebSocketHandler {

    // Track frontend sessions
    private final Set<WebSocketSession> frontendSessions = Collections.synchronizedSet(new HashSet<>());
    // Track simulator sessions
    private final Set<WebSocketSession> simulatorSessions = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String uri = Objects.requireNonNull(session.getUri()).toString();

        if (uri.contains("/frontend")) {
            frontendSessions.add(session);
            System.out.println("Frontend connected: " + session.getId());
            notifySimulators("FRONTEND_CONNECTED");
        } else if (uri.contains("/ws-sensor-data")) {
            simulatorSessions.add(session);
            System.out.println("Simulator connected: " + session.getId());
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        String uri = Objects.requireNonNull(session.getUri()).toString();

        if (uri.contains("/ws-sensor-data")) {
            // Data from simulators → forward to frontends
            synchronized (frontendSessions) {
                for (WebSocketSession frontend : frontendSessions) {
                    if (frontend.isOpen()) {
                        try {
                            frontend.sendMessage(message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String uri = Objects.requireNonNull(session.getUri()).toString();

        if (uri.contains("/frontend")) {
            frontendSessions.remove(session);
            System.out.println("Frontend disconnected: " + session.getId());

            if (frontendSessions.isEmpty()) {
                notifySimulators("FRONTEND_DISCONNECTED");
            }
        } else if (uri.contains("/ws-sensor-data")) {
            simulatorSessions.remove(session);
            System.out.println("Simulator disconnected: " + session.getId());
        }
    }

    private void notifySimulators(String msg) {
        synchronized (simulatorSessions) {
            for (WebSocketSession simulator : simulatorSessions) {
                if (simulator.isOpen()) {
                    try {
                        simulator.sendMessage(new TextMessage(msg));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

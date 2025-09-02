package com.pratik.IotAnalyser.webSocket;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WebSocketSessionTracker {
    private final AtomicInteger activeSessions = new AtomicInteger(0);

    public void addSession() {
        activeSessions.incrementAndGet();
    }

    public void removeSession() {
        activeSessions.decrementAndGet();
    }

    public int getActiveSessions() {
        return activeSessions.get();
    }

    public boolean hasActiveSessions() {
        return activeSessions.get() > 0;
    }
}

//package com.pratik.IotAnalyser.simulator;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.pratik.IotAnalyser.dtos.sensorDto.SensorRegistrationDto;
//import com.pratik.IotAnalyser.model.SensorData;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.messaging.converter.MappingJackson2MessageConverter;
//import org.springframework.messaging.simp.stomp.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.socket.client.standard.StandardWebSocketClient;
//import org.springframework.web.socket.messaging.WebSocketStompClient;
//import org.springframework.web.socket.sockjs.client.SockJsClient;
//import org.springframework.web.socket.sockjs.client.Transport;
//import org.springframework.web.socket.sockjs.client.WebSocketTransport;
//
//import java.lang.reflect.Type;
//import java.util.Collections;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.TimeoutException;
//
//@Service
//public class WebSocketClientService {
//
//    private static final Logger log = LoggerFactory.getLogger(WebSocketClientService.class);
//    private final WebSocketStompClient stompClient;
//    private final ObjectMapper objectMapper;
//
//    /**
//     * Single constructor — Spring will inject the application ObjectMapper bean (which should
//     * be configured to register JavaTimeModule so LocalDateTime serializes properly).
//     *
//     * Uses SockJS transport (matches server-side .withSockJS()).
//     */
//    public WebSocketClientService(ObjectMapper objectMapper) {
//        this.objectMapper = objectMapper;
//
//        // Use SockJS client (good when server registered .withSockJS())
//        List<Transport> transports = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
//        SockJsClient sockJsClient = new SockJsClient(transports);
//
//        this.stompClient = new WebSocketStompClient(sockJsClient);
//
//        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
//        converter.setObjectMapper(this.objectMapper); // << wire injected mapper
//        this.stompClient.setMessageConverter(converter);
//    }
//
//    /**
//     * Blocking connect that returns a connected StompSession (null on failure).
//     * When using SockJS client, pass an HTTP URL (e.g. "http://localhost:8080/ws-sensor-data").
//     */
//    public StompSession connect(String serverUrl) {
//        if (serverUrl == null || serverUrl.isBlank()) {
//            log.error("WebSocketClientService.connect called with null/empty serverUrl");
//            return null;
//        }
//
//        try {
//            CompletableFuture<StompSession> future = stompClient.connectAsync(serverUrl, new StompSessionHandlerAdapter() {
//                @Override
//                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
//                    log.info("Connected to {}", serverUrl);
//                }
//
//                @Override
//                public void handleTransportError(StompSession session, Throwable exception) {
//                    log.error("Transport error: {}", exception.getMessage(), exception);
//                }
//
//                @Override
//                public Type getPayloadType(StompHeaders headers) {
//                    return byte[].class;
//                }
//            });
//
//            return future.get(10, TimeUnit.SECONDS);
//        } catch (InterruptedException ie) {
//            Thread.currentThread().interrupt();
//            log.error("WebSocket connection interrupted", ie);
//            return null;
//        } catch (ExecutionException ee) {
//            log.error("WebSocket connection failed (execution): {}", ee.getMessage(), ee);
//            return null;
//        } catch (TimeoutException te) {
//            log.error("WebSocket connection timed out after 10s: {}", te.getMessage(), te);
//            return null;
//        }
//    }
//
//    /**
//     * Non-blocking connect — returns CompletableFuture so caller can attach callbacks.
//     */
//    public CompletableFuture<StompSession> connectAsyncNonBlocking(String serverUrl, StompSessionHandler handler) {
//        StompSessionHandler sessionHandler = handler != null ? handler : new StompSessionHandlerAdapter() {
//            @Override
//            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
//                log.info("Connected (async) to {}", serverUrl);
//            }
//
//            @Override
//            public void handleTransportError(StompSession session, Throwable exception) {
//                log.error("Transport error (async): {}", exception.getMessage(), exception);
//            }
//
//            @Override
//            public Type getPayloadType(StompHeaders headers) {
//                return byte[].class;
//            }
//        };
//        return stompClient.connectAsync(serverUrl, sessionHandler);
//    }
//
//    public void sendSensorData(StompSession session, String destination, SensorData sensorData) {
//        if (session != null && session.isConnected()) {
//            session.send(destination, sensorData);
//            log.info("Sent SensorData: {}", sensorData);
//        } else {
//            log.warn("Cannot send sensor data — session null or not connected");
//        }
//    }
//
//    public void registerDevice(StompSession session, String destination, SensorRegistrationDto dto) {
//        if (session != null && session.isConnected()) {
//            session.send(destination, dto);
//            log.info("Registered Device: {}", dto);
//        } else {
//            log.warn("Cannot register device — session null or not connected");
//        }
//    }
//}

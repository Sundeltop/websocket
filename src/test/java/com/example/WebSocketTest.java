package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WebSocketTest {

    private final WebSocketConfig config = WebSocketConfig.builder()
            .uri(URI.create("wss://echo.websocket.org"))
            .timeoutInMillis(10_000L)
            .build();

    private WebSocketClientImpl webSocketClient;

    @BeforeEach
    void setupClient() {
        webSocketClient = new WebSocketClientImpl(config);
    }

    @Test
    void verifySendAndReceiveWebsocketMessage() {
        final String expectedMessage = "Hello from WebSocket!";

        final String receivedMessage = webSocketClient.connectAndListenFor(expectedMessage);
        assertEquals(expectedMessage, receivedMessage);
    }

    @Test
    void verifyPerformActionInWebsocket() {
        final AtomicInteger counter = new AtomicInteger();

        webSocketClient.connectAndPerform(counter::getAndIncrement);
        assertEquals(1, counter.get());
    }
}

package com.example;

import lombok.extern.log4j.Log4j2;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;

@Log4j2
public class WebSocketClientImpl extends WebSocketClient {

    private final WebSocketConfig config;
    private Instant connectionOpenedTime;
    private ConcurrentLinkedQueue<String> receivedMessagesQueue;

    public WebSocketClientImpl(WebSocketConfig config) {
        super(config.uri());
        this.config = config;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        connectionOpenedTime = Instant.now();
        receivedMessagesQueue = new ConcurrentLinkedQueue<>();
        log.info("Connected to websocket on {}", config.uri());
    }

    @Override
    public void onMessage(String message) {
        receivedMessagesQueue.add(message);
        log.info("Received message: {}", message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("Closed connection to websocket with code {}. Reason: {}", code, reason);
    }

    @Override
    public void onError(Exception e) {
        throw new RuntimeException(e);
    }

    public String connectAndListenFor(String expectedMessage) {
        boolean isMessageSent = false;
        try {
            connectBlocking();

            while (!isClosed()) {

                if (!isMessageSent) {
                    send(expectedMessage);
                    isMessageSent = true;
                }

                if (isMessageReceived(receivedMessagesQueue, expectedMessage)) {
                    closeConnection(1000, "Received expected message");
                    break;
                }

                if (Instant.now().isAfter(getAllowedAliveTime())) {
                    closeConnection(1006, "Unable to receive expected message within the specified timeframe");
                }
            }
        } catch (InterruptedException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
        return extractMessage(receivedMessagesQueue, expectedMessage);
    }

    public void connectAndPerform(Runnable runnable) {
        try {
            connectBlocking();

            while (!isClosed()) {
                runnable.run();
                closeConnection(1000, "Performed action");
            }
        } catch (InterruptedException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    private boolean isMessageReceived(ConcurrentLinkedQueue<String> receivedMessages, String expectedMessage) {
        return receivedMessages.stream().anyMatch(message -> message.contains(expectedMessage));
    }

    private Instant getAllowedAliveTime() {
        return connectionOpenedTime.plusMillis(config.timeoutInMillis());
    }

    private String extractMessage(ConcurrentLinkedQueue<String> receivedMessages, String expectedMessage) {
        return receivedMessages.stream()
                .filter(message -> message.contains(expectedMessage))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Expected message '%s' was not received from websocket".formatted(expectedMessage)));
    }
}

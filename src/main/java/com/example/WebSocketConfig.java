package com.example;

import lombok.Builder;
import lombok.NonNull;

import java.net.URI;

@Builder
public record WebSocketConfig(@NonNull URI uri, @NonNull Long timeoutInMillis) {
}

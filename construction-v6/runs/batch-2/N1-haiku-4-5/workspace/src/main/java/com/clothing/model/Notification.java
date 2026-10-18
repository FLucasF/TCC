package com.clothing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record Notification(
    long id,
    NotificationType type,
    @JsonProperty("orderId")
    long orderId,
    String status,
    @JsonProperty("createdAt")
    Instant createdAt
) {}

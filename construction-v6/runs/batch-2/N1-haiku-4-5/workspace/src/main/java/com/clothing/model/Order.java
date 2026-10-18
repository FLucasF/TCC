package com.clothing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record Order(
    long id,
    OrderStatus status,
    @JsonProperty("createdAt")
    Instant createdAt,
    Summary summary,
    List<OrderItem> items,
    @JsonProperty("trackingCode")
    String trackingCode
) {}

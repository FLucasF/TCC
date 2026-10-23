package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MessageResponse(
    @JsonProperty("id") long id,
    @JsonProperty("channel") String channel,
    @JsonProperty("to") String to,
    @JsonProperty("subject") String subject,
    @JsonProperty("body") String body,
    @JsonProperty("status") String status
) {}

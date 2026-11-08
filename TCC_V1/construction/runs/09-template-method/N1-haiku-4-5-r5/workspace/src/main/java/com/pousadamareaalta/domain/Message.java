package com.pousadamareaalta.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Message(
    @JsonProperty("id")
    long id,
    @JsonProperty("channel")
    String channel,
    @JsonProperty("to")
    String to,
    @JsonProperty("subject")
    String subject,
    @JsonProperty("body")
    String body
) {}

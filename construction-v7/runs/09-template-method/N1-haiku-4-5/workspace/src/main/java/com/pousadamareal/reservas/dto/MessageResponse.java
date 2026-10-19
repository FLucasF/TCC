package com.pousadamareal.reservas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MessageResponse {
    @JsonProperty("id")
    public long id;

    @JsonProperty("channel")
    public String channel;

    @JsonProperty("to")
    public String to;

    @JsonProperty("subject")
    public String subject;

    @JsonProperty("body")
    public String body;
}

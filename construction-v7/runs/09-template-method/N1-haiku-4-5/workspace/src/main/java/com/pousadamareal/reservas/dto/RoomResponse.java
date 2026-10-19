package com.pousadamareal.reservas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class RoomResponse {
    @JsonProperty("number")
    public int number;

    @JsonProperty("type")
    public String type;

    @JsonProperty("capacity")
    public int capacity;

    @JsonProperty("nightlyRate")
    public BigDecimal nightlyRate;
}

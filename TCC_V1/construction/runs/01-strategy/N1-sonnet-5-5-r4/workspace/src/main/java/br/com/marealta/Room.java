package br.com.marealta;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record Room(int number, RoomType type) {

    @JsonProperty("capacity")
    public int capacity() {
        return type.capacity();
    }

    @JsonProperty("nightlyRate")
    public BigDecimal nightlyRate() {
        return type.nightlyRate();
    }
}

package br.com.marealta;

import java.math.BigDecimal;

public record Room(int number, RoomType type) {
    public int getNumber() { return number; }
    public RoomType getType() { return type; }
    public int getCapacity() { return type.capacity(); }
    public BigDecimal getNightlyRate() { return type.nightlyRate(); }
}

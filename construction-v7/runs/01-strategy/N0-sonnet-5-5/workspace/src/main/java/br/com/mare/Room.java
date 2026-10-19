package br.com.mare;

import java.math.BigDecimal;

public record Room(int number, Room.RoomType type, int capacity, BigDecimal nightlyRate) {
    public enum RoomType { STANDARD, DELUXE, SUITE }
}

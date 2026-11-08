package com.pousada.marealta.domain.room;

import com.pousada.marealta.domain.shared.Money;
import java.util.Objects;

public class Room {
    private final int number;
    private final RoomType type;
    private final Money nightlyRate;

    public Room(int number, RoomType type, Money nightlyRate) {
        Objects.requireNonNull(type, "Room type cannot be null");
        Objects.requireNonNull(nightlyRate, "Nightly rate cannot be null");

        if (number < 100 || number > 399) {
            throw new IllegalArgumentException("Room number must be between 100 and 399");
        }
        if (nightlyRate.doubleValue() <= 0) {
            throw new IllegalArgumentException("Nightly rate must be positive");
        }

        this.number = number;
        this.type = type;
        this.nightlyRate = nightlyRate;
    }

    public int number() {
        return number;
    }

    public RoomType type() {
        return type;
    }

    public int capacity() {
        return type.capacity();
    }

    public Money nightlyRate() {
        return nightlyRate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room)) return false;
        Room room = (Room) o;
        return number == room.number;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number);
    }

    @Override
    public String toString() {
        return number + " (" + type.displayName() + ")";
    }
}

package br.com.marealta.domain;

public enum RoomType {
    STANDARD(2, Money.of("249.90")),
    DELUXE(3, Money.of("389.90")),
    SUITE(4, Money.of("579.90"));

    private final int capacity;
    private final Money nightlyRate;

    RoomType(int capacity, Money nightlyRate) {
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }

    public int getCapacity() {
        return capacity;
    }

    public Money getNightlyRate() {
        return nightlyRate;
    }
}

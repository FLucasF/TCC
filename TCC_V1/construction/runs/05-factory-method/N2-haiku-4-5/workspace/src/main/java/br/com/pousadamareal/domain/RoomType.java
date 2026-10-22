package br.com.pousadamareal.domain;

public enum RoomType {
    STANDARD("STANDARD", 2, 249.90),
    DELUXE("DELUXE", 3, 389.90),
    SUITE("SUITE", 4, 579.90);

    private final String name;
    private final int capacity;
    private final double nightlyRate;

    RoomType(String name, int capacity, double nightlyRate) {
        this.name = name;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }

    public String displayName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getNightlyRate() {
        return nightlyRate;
    }
}

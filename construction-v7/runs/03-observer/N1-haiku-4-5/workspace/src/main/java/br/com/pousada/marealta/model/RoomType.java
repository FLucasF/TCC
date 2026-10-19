package br.com.pousada.marealta.model;

public enum RoomType {
    STANDARD("STANDARD", 2, 249.90),
    DELUXE("DELUXE", 3, 389.90),
    SUITE("SUITE", 4, 579.90);

    private final String displayName;
    private final int capacity;
    private final double nightlyRate;

    RoomType(String displayName, int capacity, double nightlyRate) {
        this.displayName = displayName;
        this.capacity = capacity;
        this.nightlyRate = nightlyRate;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getNightlyRate() {
        return nightlyRate;
    }
}

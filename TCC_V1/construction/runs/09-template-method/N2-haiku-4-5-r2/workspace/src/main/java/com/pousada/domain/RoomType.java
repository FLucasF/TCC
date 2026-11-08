package com.pousada.domain;

public enum RoomType {
    STANDARD("STANDARD", 249.90, 2),
    DELUXE("DELUXE", 389.90, 3),
    SUITE("SUITE", 579.90, 4);

    private final String displayName;
    private final double nightly_rate;
    private final int capacity;

    RoomType(String displayName, double nightly_rate, int capacity) {
        this.displayName = displayName;
        this.nightly_rate = nightly_rate;
        this.capacity = capacity;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getNightlyRate() {
        return nightly_rate;
    }

    public int getCapacity() {
        return capacity;
    }
}

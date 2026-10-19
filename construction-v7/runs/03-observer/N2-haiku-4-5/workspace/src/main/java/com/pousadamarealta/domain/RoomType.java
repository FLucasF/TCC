package com.pousadamarealta.domain;

import java.math.BigDecimal;

public enum RoomType {
    STANDARD("STANDARD", 2, new BigDecimal("249.90")),
    DELUXE("DELUXE", 3, new BigDecimal("389.90")),
    SUITE("SUITE", 4, new BigDecimal("579.90"));

    public final String displayName;
    public final int capacity;
    public final BigDecimal nightly;

    RoomType(String displayName, int capacity, BigDecimal nightly) {
        this.displayName = displayName;
        this.capacity = capacity;
        this.nightly = nightly;
    }
}

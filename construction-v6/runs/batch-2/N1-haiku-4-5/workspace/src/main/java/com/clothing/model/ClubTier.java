package com.clothing.model;

public enum ClubTier {
    BRONZE,
    SILVER,
    GOLD;

    public static ClubTier fromString(String value) {
        if (value == null) return null;
        try {
            return ClubTier.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

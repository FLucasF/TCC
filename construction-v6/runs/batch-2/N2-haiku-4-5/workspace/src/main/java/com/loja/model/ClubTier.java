package com.loja.model;

public enum ClubTier {
    BRONZE, SILVER, GOLD;

    public static ClubTier fromString(String value) {
        try {
            return ClubTier.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

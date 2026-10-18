package com.loja.model;

public enum Region {
    SOUTHEAST, SOUTH, MIDWEST, NORTH, NORTHEAST;

    public static Region fromString(String value) {
        try {
            return Region.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

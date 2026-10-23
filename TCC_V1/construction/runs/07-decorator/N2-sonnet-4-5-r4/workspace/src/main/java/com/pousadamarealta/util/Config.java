package com.pousadamarealta.util;

public class Config {

    public static String get(String key) {
        String value = System.getProperty(key);
        if (value != null) {
            return value;
        }
        return System.getenv(key);
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }
}

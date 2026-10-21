package com.pousadamarealta.config;

public class Config {
    public static boolean isTestMode() {
        return "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
    }

    public static int getPort() {
        String port = System.getenv("PORT");
        return port != null ? Integer.parseInt(port) : 7070;
    }

    public static boolean isArchiveCopyEnabled() {
        return true;
    }
}

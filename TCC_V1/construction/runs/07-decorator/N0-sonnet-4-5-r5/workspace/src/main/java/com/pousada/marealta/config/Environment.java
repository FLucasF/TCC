package com.pousada.marealta.config;

public class Environment {
    public static String getDbUrl() {
        return System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/marealta");
    }

    public static String getDbUser() {
        return System.getenv().getOrDefault("DB_USER", "postgres");
    }

    public static String getDbPassword() {
        return System.getenv().getOrDefault("DB_PASSWORD", "postgres");
    }

    public static int getPort() {
        String port = System.getenv("PORT");
        return port != null ? Integer.parseInt(port) : 7070;
    }

    public static boolean isTestMode() {
        return "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
    }
}

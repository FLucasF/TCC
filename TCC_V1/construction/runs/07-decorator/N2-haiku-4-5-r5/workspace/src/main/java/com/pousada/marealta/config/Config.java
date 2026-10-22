package com.pousada.marealta.config;

public class Config {
    public static String getDbUrl() {
        return getEnv("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
    }

    public static String getDbUser() {
        return getEnv("DB_USER", "postgres");
    }

    public static String getDbPassword() {
        return getEnv("DB_PASSWORD", "");
    }

    public static int getPort() {
        return Integer.parseInt(getEnv("PORT", "7070"));
    }

    public static boolean isTestMode() {
        return "true".equalsIgnoreCase(getEnv("HOTEL_TEST_MODE", "false"));
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}

package com.pousada.marealta.config;

public class AppConfig {
    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static int port;
    private static boolean testMode;

    public static void load() {
        dbUrl = getEnv("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        dbUser = getEnv("DB_USER", "postgres");
        dbPassword = getEnv("DB_PASSWORD", "");
        port = Integer.parseInt(getEnv("PORT", "7070"));
        testMode = "true".equalsIgnoreCase(getEnv("HOTEL_TEST_MODE", "false"));
    }

    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }

    public static String getDbUrl() {
        return dbUrl;
    }

    public static String getDbUser() {
        return dbUser;
    }

    public static String getDbPassword() {
        return dbPassword;
    }

    public static int getPort() {
        return port;
    }

    public static boolean isTestMode() {
        return testMode;
    }
}

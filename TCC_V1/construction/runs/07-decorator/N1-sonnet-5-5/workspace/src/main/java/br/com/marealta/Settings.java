package br.com.marealta;

import java.util.Map;

public record Settings(int port, String dbUrl, String dbUser, String dbPassword, boolean testMode) {

    public static Settings fromEnvironment(Map<String, String> env) {
        String port = env.get("PORT");
        return new Settings(
                port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()),
                env.get("DB_URL"), env.get("DB_USER"), env.get("DB_PASSWORD"),
                "true".equalsIgnoreCase(String.valueOf(env.get("HOTEL_TEST_MODE")).trim()));
    }
}

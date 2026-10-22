package br.com.marealta;

import java.util.Map;

public record AppConfig(int port, String dbUrl, String dbUser, String dbPassword, boolean testMode,
                        String messageRules) {

    public static AppConfig fromEnv(Map<String, String> env) {
        String port = env.get("PORT");
        return new AppConfig(
                port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()),
                env.get("DB_URL"),
                env.get("DB_USER"),
                env.get("DB_PASSWORD"),
                "true".equalsIgnoreCase(env.getOrDefault("HOTEL_TEST_MODE", "").trim()),
                env.get("HOTEL_MESSAGE_RULES"));
    }
}

package br.com.pousadamarealta.reservas.web;

public record Config(int port, boolean testMode) {

    public static Config fromEnvironment() {
        String port = System.getenv("PORT");
        return new Config(
                port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()),
                "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE")));
    }
}

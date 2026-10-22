package br.com.maralta.pousada;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        String url = require("DB_URL");
        Database database = new Database(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        Api.create(new ReservationService(database), new Reports(database)).start(port);
    }

    private static String require(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente ausente: " + name);
        }
        return value;
    }
}

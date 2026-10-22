package pousada;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        String url = require("DB_URL");
        Database db = new Database(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        String port = System.getenv("PORT");
        new Api(new ReservationService(db)).start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }

    private static String require(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Variável de ambiente ausente: " + name);
        return value;
    }
}

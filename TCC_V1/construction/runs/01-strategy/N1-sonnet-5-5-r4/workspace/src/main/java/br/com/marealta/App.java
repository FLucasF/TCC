package br.com.marealta;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        String port = System.getenv("PORT");
        Database db = new Database(required("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        new Routes(new ReservationService(db), new ReportService(db))
                .build()
                .start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente ausente: " + name);
        }
        return value;
    }
}

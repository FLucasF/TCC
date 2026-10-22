package br.com.marealta;

import br.com.marealta.persistence.Database;
import br.com.marealta.web.Routes;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Database db = new Database(required("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        db.initialize();
        String port = System.getenv("PORT");
        Routes.create(new ReservationService(db)).start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente " + name + " é obrigatória");
        }
        return value;
    }
}

package pousada;

import pousada.persistence.Database;
import pousada.service.ReservationService;
import pousada.web.Api;

public class App {
    public static void main(String[] args) {
        Database database = new Database(
                require("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        database.createSchema();

        String port = System.getenv("PORT");
        new Api(new ReservationService(database)).create()
                .start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }

    private static String require(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente " + name + " não definida.");
        }
        return value;
    }
}

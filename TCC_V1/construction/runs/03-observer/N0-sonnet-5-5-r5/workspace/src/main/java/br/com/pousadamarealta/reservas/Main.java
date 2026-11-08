package br.com.pousadamarealta.reservas;

import io.javalin.Javalin;
import br.com.pousadamarealta.reservas.persistence.Database;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        String url = require("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");
        String port = System.getenv("PORT");

        Database db = new Database(url, user, password);
        Javalin app = App.create(db);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            db.close();
        }));
        app.start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }

    private static String require(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente " + name + " não definida");
        }
        return value;
    }
}

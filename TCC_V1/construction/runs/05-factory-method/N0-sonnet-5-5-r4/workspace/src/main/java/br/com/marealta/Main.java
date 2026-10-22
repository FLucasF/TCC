package br.com.marealta;

import javax.sql.DataSource;

public final class Main {
    public static void main(String[] args) {
        String url = require("DB_URL");
        DataSource ds = Db.connect(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        String port = System.getenv("PORT");
        Api.create(new ReservationService(new Store(ds))).start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }

    private static String require(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) throw new IllegalStateException("Variável de ambiente " + name + " é obrigatória");
        return v;
    }
}

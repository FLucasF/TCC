package pousada;

import com.zaxxer.hikari.HikariDataSource;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        String url = require("DB_URL");
        HikariDataSource ds = Db.connect(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        Db.migrate(ds);
        String port = System.getenv("PORT");
        Api.create(new ReservationService(ds)).start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }

    private static String require(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) throw new IllegalStateException("Variável de ambiente " + name + " não definida");
        return v;
    }
}

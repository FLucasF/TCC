package br.com.marealta;

import br.com.marealta.db.Database;
import br.com.marealta.service.ReportService;
import br.com.marealta.service.ReservationService;
import br.com.marealta.web.App;

public class Main {
    public static void main(String[] args) {
        String url = require("DB_URL");
        Database db = new Database(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        db.init();
        String port = System.getenv("PORT");
        new App(new ReservationService(db), new ReportService(db))
                .create()
                .start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }

    private static String require(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) throw new IllegalStateException("Variável de ambiente " + name + " não definida");
        return v;
    }
}

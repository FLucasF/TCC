package br.com.marealta;

import br.com.marealta.web.Api;
import io.javalin.Javalin;

public final class App {
    public static void main(String[] args) {
        String url = require("DB_URL");
        Db db = new Db(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        db.init();

        Repository repo = new Repository();
        Api api = new Api(new ReservationService(db, repo), new ReportService(db, repo));

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);
        api.register(app);
        app.start(port);
    }

    private static String require(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) throw new IllegalStateException("Variável de ambiente " + name + " não definida");
        return v;
    }
}

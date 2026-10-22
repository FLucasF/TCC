package br.com.marealte;

import br.com.marealte.database.DatabaseSetup;
import br.com.marealte.routes.Routes;
import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        DatabaseSetup.initialize();

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(port);

        Routes.register(app);
    }
}

package com.marealta;

import io.javalin.Javalin;
import com.marealta.database.Database;
import com.marealta.api.Routes;

public class Application {
    public static void main(String[] args) {
        Database.initialize();

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        Javalin app = Javalin.create().start(port);

        Routes.register(app);

        System.out.println("Pousada Maré Alta system started on port " + port);
    }
}

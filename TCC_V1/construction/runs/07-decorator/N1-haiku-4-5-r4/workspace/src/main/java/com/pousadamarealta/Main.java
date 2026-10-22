package com.pousadamarealta;

import io.javalin.Javalin;
import com.pousadamarealta.db.Database;
import com.pousadamarealta.api.ApiRoutes;

public class Main {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.err.println("Database environment variables not set");
            System.exit(1);
        }

        Database.initialize(dbUrl, dbUser, dbPassword);

        Javalin app = Javalin.create();
        ApiRoutes.setupRoutes(app);
        app.start(port);
    }
}

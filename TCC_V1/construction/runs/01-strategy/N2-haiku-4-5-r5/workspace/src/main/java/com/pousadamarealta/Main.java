package com.pousadamarealta;

import io.javalin.Javalin;
import com.pousadamarealta.database.Database;
import com.pousadamarealta.api.Routes;

public class Main {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalArgumentException("DB_URL, DB_USER, and DB_PASSWORD must be set");
        }

        Database.initialize(dbUrl, dbUser, dbPassword);

        Javalin app = Javalin.create().start(port);
        Routes.setup(app);
    }
}

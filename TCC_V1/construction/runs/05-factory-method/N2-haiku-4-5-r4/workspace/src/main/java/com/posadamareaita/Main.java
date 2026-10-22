package com.posadamareaita;

import com.posadamareaita.api.Api;
import com.posadamareaita.database.Database;
import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) throws Exception {
        // Get environment variables
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        // Initialize database
        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalArgumentException("Missing database configuration");
        }

        Database.initialize(dbUrl, dbUser, dbPassword);

        // Start server
        Javalin app = Javalin.create()
                .start(port);

        // Setup routes
        Api.setupRoutes(app);

        System.out.println("Server started on port " + port);
    }
}

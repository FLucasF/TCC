package com.pousada;

import com.pousada.api.ApiRoutes;
import com.pousada.database.Database;
import io.javalin.Javalin;

public class Application {
    public static void main(String[] args) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.err.println("Missing required environment variables: DB_URL, DB_USER, DB_PASSWORD");
            System.exit(1);
        }

        int port = portStr != null ? Integer.parseInt(portStr) : 7070;

        Database database = new Database(dbUrl, dbUser, dbPassword);
        ApiRoutes apiRoutes = new ApiRoutes(database);

        Javalin app = Javalin.create()
            .start(port);

        apiRoutes.register(app);

        System.out.println("Pousada Maré Alta server started on port " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            database.close();
        }));
    }
}

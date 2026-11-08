package com.pousada;

import com.pousada.api.ApiController;
import com.pousada.repository.Database;
import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Database.init(dbUrl, dbUser, dbPassword);

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(port);

        ApiController controller = new ApiController();
        controller.registerRoutes(app);

        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Servidor rodando na porta " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            Database.close();
        }));
    }
}

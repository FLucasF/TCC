package com.pousada;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        Database.init();

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        Javalin app = Javalin.create().start(port);

        logger.info("Server started on port {}", port);

        RouteConfig.setupRoutes(app);
    }
}

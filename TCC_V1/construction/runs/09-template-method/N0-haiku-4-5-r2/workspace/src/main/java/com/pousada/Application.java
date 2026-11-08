package com.pousada;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        int port = getPort();

        try {
            Database.init();
            logger.info("Database initialized");
        } catch (Exception e) {
            logger.error("Failed to initialize database", e);
            System.exit(1);
        }

        Javalin app = Javalin.create(config -> {
            config.http.generateEtags = false;
        }).start(port);

        logger.info("Server starting on port {}", port);

        Api.register(app);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        return portEnv != null ? Integer.parseInt(portEnv) : 7070;
    }
}

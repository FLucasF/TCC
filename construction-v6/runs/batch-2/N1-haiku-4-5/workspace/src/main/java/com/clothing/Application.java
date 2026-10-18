package com.clothing;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        Database.initialize();

        var app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(port);

        logger.info("Service started on port {}", port);

        new OrderRoutes(app).register();
        new ProductRoutes(app).register();
        new CustomerRoutes(app).register();
    }
}

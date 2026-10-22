package com.pousada.marealta;

import com.pousada.marealta.database.Database;
import com.pousada.marealta.handlers.ReservationHandler;
import com.pousada.marealta.handlers.RoomHandler;
import com.pousada.marealta.handlers.MessageHandler;
import com.pousada.marealta.handlers.ReportHandler;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        // Initialize database
        Database.initialize();

        // Get port from environment or use default
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        // Create and configure Javalin app
        var app = Javalin.create()
            .exception(Exception.class, (e, ctx) -> {
                logger.error("Unhandled exception", e);
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", "Internal server error"));
            });

        // Register handlers
        RoomHandler.register(app);
        ReservationHandler.register(app);
        MessageHandler.register(app);
        ReportHandler.register(app);

        app.start(port);
        logger.info("Server started on port {}", port);
    }

    record ErrorResponse(String error, String message) {}
}

package br.pousada.marealta;

import io.javalin.Javalin;
import br.pousada.marealta.database.Database;
import br.pousada.marealta.api.RoomHandler;
import br.pousada.marealta.api.ReservationHandler;
import br.pousada.marealta.api.MessageHandler;
import br.pousada.marealta.api.ReportHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        logger.info("Initializing application on port {}", port);

        try {
            Database.init();
            logger.info("Database initialized");
        } catch (Exception e) {
            logger.error("Failed to initialize database", e);
            System.exit(1);
        }

        Javalin app = Javalin.create();

        RoomHandler.register(app);
        ReservationHandler.register(app);
        MessageHandler.register(app);
        ReportHandler.register(app);

        app.start(port);
        logger.info("Application started successfully on port {}", port);
    }
}

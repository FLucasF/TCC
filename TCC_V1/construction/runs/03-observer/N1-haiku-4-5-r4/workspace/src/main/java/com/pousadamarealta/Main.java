package com.pousadamarealta;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pousadamarealta.database.Database;
import com.pousadamarealta.routes.RoomRoutes;
import com.pousadamarealta.routes.ReservationRoutes;
import com.pousadamarealta.routes.MessageRoutes;
import com.pousadamarealta.routes.ReportRoutes;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            Database.init();

            int port = getPort();
            Javalin app = Javalin.create().start(port);

            RoomRoutes.register(app);
            ReservationRoutes.register(app);
            MessageRoutes.register(app);
            ReportRoutes.register(app);

            app.exception(Exception.class, (e, ctx) -> {
                logger.error("Unexpected error", e);
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR", "Internal server error"));
            });

            logger.info("Server started on port {}", port);
        } catch (Exception e) {
            logger.error("Failed to start server", e);
            System.exit(1);
        }
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        return portEnv != null ? Integer.parseInt(portEnv) : 7070;
    }
}

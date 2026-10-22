package com.pousada;

import com.pousada.api.ReservationApi;
import com.pousada.api.ReportApi;
import com.pousada.api.RoomApi;
import com.pousada.api.MessageApi;
import com.pousada.db.Database;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            Database.initialize();
            log.info("Database initialized");

            int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
            Javalin app = Javalin.create()
                    .exception(Exception.class, (e, ctx) -> {
                        log.error("Unhandled exception", e);
                        ctx.status(500).result("Internal server error");
                    });

            RoomApi.register(app);
            ReservationApi.register(app);
            MessageApi.register(app);
            ReportApi.register(app);

            app.start(port);
            log.info("Server started on port {}", port);
        } catch (Exception e) {
            log.error("Failed to start application", e);
            System.exit(1);
        }
    }
}

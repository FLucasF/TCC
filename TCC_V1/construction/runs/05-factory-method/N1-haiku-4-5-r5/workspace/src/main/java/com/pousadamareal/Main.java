package com.pousadamareal;

import com.pousadamareal.db.Database;
import com.pousadamareal.handler.*;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        String port = System.getenv("PORT");
        int serverPort = port != null ? Integer.parseInt(port) : 7070;

        logger.info("Iniciando Pousada Maré Alta");

        Database.initialize();

        Javalin app = Javalin.create()
            .get("/rooms", RoomHandler::listRooms)
            .post("/reservations", ReservationHandler::create)
            .get("/reservations/:id", ReservationHandler::get)
            .post("/reservations/:id/confirmation", ReservationHandler::confirm)
            .post("/reservations/:id/cancellation", ReservationHandler::cancel)
            .post("/reservations/:id/check-in", ReservationHandler::checkIn)
            .post("/reservations/:id/check-out", ReservationHandler::checkOut)
            .get("/reservations/:id/document", ReservationHandler::getDocument)
            .get("/messages", MessageHandler::listMessages)
            .get("/reports/occupancy", ReportHandler::occupancy)
            .get("/reports/revenue", ReportHandler::revenue)
            .exception(Exception.class, (e, ctx) -> {
                logger.error("Erro não esperado", e);
                ctx.status(500).json(jsonMap("error", "INTERNAL_ERROR", "message", "Erro interno do servidor"));
            });

        app.start(serverPort);
        logger.info("Servidor iniciado na porta {}", serverPort);
    }

    public static java.util.Map<String, Object> jsonMap(Object... pairs) {
        var map = new java.util.LinkedHashMap<String, Object>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }
}

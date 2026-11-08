package br.com.pousada;

import br.com.pousada.config.DatabaseConfig;
import br.com.pousada.controller.*;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        DatabaseConfig.initializeDatabase();

        Javalin app = Javalin.create()
                .start(port);

        logger.info("Sistema iniciado na porta {}", port);

        app.exception(Exception.class, (e, ctx) -> {
            logger.error("Erro não tratado", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Erro interno do servidor"));
        });

        RoomController.routes(app);
        ReservationController.routes(app);
        MessageController.routes(app);
        ReportController.routes(app);
    }

    static class ErrorResponse {
        public String error;
        public String message;

        ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}

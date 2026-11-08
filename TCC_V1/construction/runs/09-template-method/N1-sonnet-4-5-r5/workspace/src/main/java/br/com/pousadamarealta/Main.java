package br.com.pousadamarealta;

import br.com.pousadamarealta.api.*;
import br.com.pousadamarealta.database.DatabaseSetup;
import br.com.pousadamarealta.domain.message.MessageRepository;
import br.com.pousadamarealta.domain.reservation.ReservationRepository;
import br.com.pousadamarealta.domain.reservation.ReservationService;
import br.com.pousadamarealta.domain.room.RoomRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        // Configuração do banco de dados
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");

        DatabaseSetup database = new DatabaseSetup(dbUrl, dbUser, dbPassword);

        try {
            database.initialize();
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            System.exit(1);
        }

        // Repositories
        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        // Services
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, messageRepository
        );

        // Controllers
        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(
            reservationService, reservationRepository, roomRepository
        );
        MessageController messageController = new MessageController(messageRepository);
        ReportController reportController = new ReportController(reservationRepository);

        // Configurar Jackson para JSON
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Configuração da porta
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        // Criar aplicação Javalin
        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(mapper, false));
        }).start(port);

        // Rotas
        app.get("/rooms", roomController::list);

        app.post("/reservations", reservationController::create);
        app.get("/reservations/{id}", reservationController::get);
        app.post("/reservations/{id}/confirmation", reservationController::confirm);
        app.post("/reservations/{id}/cancellation", reservationController::cancel);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);

        app.get("/messages", messageController::list);

        app.get("/reports/occupancy", reportController::occupancy);
        app.get("/reports/revenue", reportController::revenue);
        app.get("/reports/tourism-tax", reportController::tourismTax);
        app.get("/reports/rate-plans", reportController::ratePlans);

        // Tratamento de exceções
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(java.util.Map.of(
                "error", "INTERNAL_ERROR",
                "message", "An internal error occurred"
            ));
        });

        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Servidor iniciado na porta " + port);
    }
}

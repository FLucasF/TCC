package com.pousadamareaalta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamareaalta.api.ApiController;
import com.pousadamareaalta.repository.Database;
import com.pousadamareaalta.repository.MessageRepository;
import com.pousadamareaalta.repository.ReservationRepository;
import com.pousadamareaalta.service.MessageService;
import com.pousadamareaalta.service.ReportService;
import com.pousadamareaalta.service.ReservationService;
import com.pousadamareaalta.service.RoomService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class App {

    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Database database = new Database(dbUrl, dbUser, dbPassword);

        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        RoomService roomService = new RoomService();
        MessageService messageService = new MessageService(messageRepository);
        ReservationService reservationService = new ReservationService(reservationRepository, roomService, messageService);
        ReportService reportService = new ReportService(reservationRepository, roomService);

        ApiController apiController = new ApiController(roomService, reservationService, messageService, reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, false));
        }).start(port);

        apiController.registerRoutes(app);

        System.out.println("Pousada Maré Alta - Sistema de Reservas");
        System.out.println("Server started on port " + port);
    }
}

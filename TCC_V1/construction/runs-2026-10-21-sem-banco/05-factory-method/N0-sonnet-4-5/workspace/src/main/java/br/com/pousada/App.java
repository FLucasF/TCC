package br.com.pousada;

import br.com.pousada.controller.ApiController;
import br.com.pousada.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class App {

    public static void main(String[] args) {
        int port = getPort();

        RoomService roomService = new RoomService();
        MessageService messageService = new MessageService();
        DocumentService documentService = new DocumentService();
        ReservationService reservationService = new ReservationService(roomService, messageService, documentService);
        ReportService reportService = new ReportService(reservationService, roomService);

        ApiController apiController = new ApiController(roomService, reservationService, messageService, reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(port);

        apiController.registerRoutes(app);

        System.out.println("Pousada Maré Alta - Sistema de Reservas rodando na porta " + port);
    }

    private static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("PORT inválida, usando 7070");
            }
        }
        return 7070;
    }
}

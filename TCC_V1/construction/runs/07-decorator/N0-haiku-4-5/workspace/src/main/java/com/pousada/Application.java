package com.pousada;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pousada.api.ApiController;
import com.pousada.database.Database;
import com.pousada.repositories.MessageRepository;
import com.pousada.repositories.ReservationRepository;
import com.pousada.repositories.RoomRepository;
import com.pousada.services.MessageService;
import com.pousada.services.ReceiptService;
import com.pousada.services.ReportService;
import com.pousada.services.ReservationService;
import io.javalin.Javalin;
import io.javalin.json.JsonMapper;
import java.lang.reflect.Type;
import java.math.BigDecimal;

public class Application {
    public static void main(String[] args) throws Exception {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String portStr = System.getenv("PORT");
        int port = (portStr != null && !portStr.isEmpty()) ? Integer.parseInt(portStr) : 7070;

        String testModeStr = System.getenv("HOTEL_TEST_MODE");
        boolean testMode = "true".equalsIgnoreCase(testModeStr);

        boolean archiveEnabled = !"false".equalsIgnoreCase(System.getenv("HOTEL_ARCHIVE_DISABLED"));

        Database database = new Database(dbUrl, dbUser, dbPassword);
        database.initialize();

        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        ReservationService reservationService = new ReservationService(reservationRepository, roomRepository);
        MessageService messageService = new MessageService(messageRepository, testMode, archiveEnabled);
        ReceiptService receiptService = new ReceiptService(roomRepository);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JsonMapper() {
                @Override
                public String toJsonString(Object obj, Type type) {
                    return gson.toJson(obj, type);
                }

                @Override
                public Object fromJsonString(String json, Type targetType) {
                    return gson.fromJson(json, targetType);
                }
            });
        });

        new ApiController(app, gson, roomRepository, reservationRepository, messageRepository,
                         reservationService, messageService, receiptService, reportService);

        app.start(port);
    }
}

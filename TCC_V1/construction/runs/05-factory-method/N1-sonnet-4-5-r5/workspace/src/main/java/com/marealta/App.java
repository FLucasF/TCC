package com.marealta;

import com.marealta.api.ApiController;
import com.marealta.api.JsonMapper;
import com.marealta.infrastructure.database.Database;
import com.marealta.infrastructure.database.ReservationRepository;
import com.marealta.infrastructure.database.RoomRepository;
import com.marealta.infrastructure.messaging.MessageRepository;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class App {
    public static void main(String[] args) {
        try {
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");
            String portEnv = System.getenv("PORT");
            int port = portEnv != null ? Integer.parseInt(portEnv) : 7070;

            if (dbUrl == null || dbUser == null || dbPassword == null) {
                throw new IllegalStateException("Database configuration missing. Please set DB_URL, DB_USER, and DB_PASSWORD environment variables.");
            }

            Database database = new Database(dbUrl, dbUser, dbPassword);

            RoomRepository roomRepository = new RoomRepository(database.getConnection());
            ReservationRepository reservationRepository = new ReservationRepository(database.getConnection());
            MessageRepository messageRepository = new MessageRepository(database.getConnection());

            roomRepository.initializeRooms();

            ApiController apiController = new ApiController(roomRepository, reservationRepository, messageRepository);

            Javalin app = Javalin.create(config -> {
                config.jsonMapper(new JavalinJackson(JsonMapper.get(), true));
            }).start(port);

            apiController.registerRoutes(app);

            System.out.println("Pousada Maré Alta API started on port " + port);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    app.stop();
                    database.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}

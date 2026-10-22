package br.com.marealta;

import br.com.marealta.messaging.Installation;
import br.com.marealta.messaging.MessagePipeline;
import br.com.marealta.messaging.MessageSink;
import br.com.marealta.persistence.Database;
import br.com.marealta.persistence.ReservationRepository;
import br.com.marealta.persistence.RoomRepository;
import br.com.marealta.persistence.StoredMessages;
import br.com.marealta.web.Routes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Database database = new Database(required("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        database.initialize();

        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
        int port = System.getenv("PORT") == null ? 7070 : Integer.parseInt(System.getenv("PORT"));

        createApp(database, testMode).start(port);
    }

    public static Javalin createApp(Database database, boolean testMode) {
        StoredMessages storedMessages = new StoredMessages(database);
        MessageSink outbox = MessagePipeline.assemble(storedMessages, Installation.mareAlta(), testMode);
        RoomRepository rooms = new RoomRepository();
        ReservationRepository reservations = new ReservationRepository();
        ReservationService service = new ReservationService(database, rooms, reservations, outbox);
        Reports reports = new Reports(database, rooms, reservations);

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));
        new Routes(service, storedMessages, reports, mapper).register(app);
        return app;
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Environment variable " + name + " is required");
        }
        return value;
    }
}

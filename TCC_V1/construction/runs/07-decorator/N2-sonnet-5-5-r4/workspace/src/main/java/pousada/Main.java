package pousada;

import pousada.messaging.MareAltaRules;
import pousada.messaging.MessagePipeline;
import pousada.messaging.MessageSink;
import pousada.persistence.Database;
import pousada.persistence.MessageRepository;
import pousada.persistence.ReservationRepository;
import pousada.persistence.RoomRepository;
import pousada.service.ReportService;
import pousada.service.ReservationService;
import pousada.web.Routes;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Database db = new Database(required("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        start(db, testMode, port);
    }

    public static io.javalin.Javalin start(Database db, boolean testMode, int port) {
        db.initialize();
        RoomRepository rooms = new RoomRepository(db);
        ReservationRepository reservations = new ReservationRepository(db);
        MessageRepository messageStore = new MessageRepository(db);

        MessageSink messages = MessagePipeline.assemble(testMode, messageStore, MareAltaRules.rules(messageStore));

        return new Routes(new ReservationService(rooms, reservations, messages),
                new ReportService(rooms, reservations), messageStore).create().start(port);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente ausente: " + name);
        }
        return value;
    }
}

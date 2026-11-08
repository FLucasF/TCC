package pousada;

import pousada.report.ReportSource;
import pousada.report.Reports;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        Db db = new Db(require("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        build(db).start(port);
    }

    public static io.javalin.Javalin build(Db db) {
        Schema.init(db);
        RoomRepository rooms = new RoomRepository();
        ReservationRepository reservations = new ReservationRepository();
        ReservationService service = new ReservationService(db, rooms, reservations, new MessageRepository());
        ReportSource source = new ReportSource(db, rooms, reservations);
        return Api.create(service, Reports.all(source));
    }

    private static String require(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente obrigatória: " + name);
        }
        return value;
    }
}

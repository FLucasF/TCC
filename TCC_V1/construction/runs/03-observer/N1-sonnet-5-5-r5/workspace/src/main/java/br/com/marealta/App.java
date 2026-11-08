package br.com.marealta;

import io.javalin.Javalin;

import java.util.List;

public class App {
    public static void main(String[] args) {
        String url = require("DB_URL");
        String port = System.getenv("PORT");
        Database db = new Database(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        create(db).start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }

    /** Monta o sistema. Quem é avisado dos eventos das reservas é listado aqui, e só aqui. */
    public static Javalin create(Database db) {
        Rooms rooms = new Rooms();
        ReservationRepository reservations = new ReservationRepository();
        List<ReservationListener> listeners = List.of(
                new GuestNotifier(rooms),
                new HousekeepingNotifier("governanca@pousadamarealta.com.br"),
                new FinanceNotifier("financeiro@pousadamarealta.com.br"));
        ReservationService service = new ReservationService(db, rooms, reservations, new MessageRepository(),
                listeners);
        ReportService reports = new ReportService(db, rooms, reservations);

        Javalin app = Javalin.create();
        new Routes(rooms, service, reports).register(app);
        return app;
    }

    private static String require(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Variável " + name + " não definida");
        return value;
    }
}

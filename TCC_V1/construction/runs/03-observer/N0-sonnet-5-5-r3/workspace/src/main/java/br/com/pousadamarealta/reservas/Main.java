package br.com.pousadamarealta.reservas;

import br.com.pousadamarealta.reservas.notification.Notifiers;
import br.com.pousadamarealta.reservas.persistence.Database;
import br.com.pousadamarealta.reservas.persistence.MessageRepository;
import br.com.pousadamarealta.reservas.persistence.ReservationRepository;
import br.com.pousadamarealta.reservas.persistence.RoomRepository;
import br.com.pousadamarealta.reservas.service.ReportService;
import br.com.pousadamarealta.reservas.service.ReservationService;
import br.com.pousadamarealta.reservas.web.App;
import io.javalin.Javalin;

public final class Main {
    private Main() {
    }

    /** Monta o sistema sobre o banco e sobe o servidor na porta dada (0 = qualquer porta livre). */
    public static Javalin start(Database database, int port) {
        database.migrate();
        RoomRepository rooms = new RoomRepository();
        ReservationRepository reservations = new ReservationRepository();
        MessageRepository messages = new MessageRepository();
        var service = new ReservationService(database, rooms, reservations, messages, Notifiers.standard());
        var reports = new ReportService(database, rooms, reservations);
        return new App(service, reports).create().start(port);
    }

    public static void main(String[] args) {
        String url = required("DB_URL");
        Database database = new Database(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        String port = System.getenv("PORT");
        start(database, port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente obrigatória ausente: " + name);
        }
        return value;
    }
}

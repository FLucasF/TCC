package br.com.marealta.pousada;

import java.util.List;

import br.com.marealta.pousada.domain.ReservationListener;
import br.com.marealta.pousada.domain.ReservationService;
import br.com.marealta.pousada.domain.RoomCatalog;
import br.com.marealta.pousada.notification.FinanceNotice;
import br.com.marealta.pousada.notification.GuestConfirmationNotice;
import br.com.marealta.pousada.notification.HousekeepingNotice;
import br.com.marealta.pousada.persistence.Database;
import br.com.marealta.pousada.persistence.JdbcMessageLog;
import br.com.marealta.pousada.persistence.JdbcReservationRepository;
import br.com.marealta.pousada.web.Api;

public final class Main {

    public static void main(String[] args) {
        Database db = new Database(required("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        db.migrate();

        RoomCatalog rooms = new RoomCatalog();
        JdbcMessageLog messages = new JdbcMessageLog(db);

        // Quem é avisado dos eventos das reservas: um novo interessado entra aqui, com uma linha.
        List<ReservationListener> listeners = List.of(
                new GuestConfirmationNotice(messages),
                new HousekeepingNotice(messages, "governanca@pousadamarealta.com.br"),
                new FinanceNotice(messages, "financeiro@pousadamarealta.com.br"));

        ReservationService service = new ReservationService(
                new JdbcReservationRepository(db, rooms), rooms, listeners);

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
        new Api(service, rooms, messages).create().start(port);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("variável de ambiente ausente: " + name);
        }
        return value;
    }
}

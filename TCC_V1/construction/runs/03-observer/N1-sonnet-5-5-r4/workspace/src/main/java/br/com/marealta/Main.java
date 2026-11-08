package br.com.marealta;

import br.com.marealta.app.ReservationService;
import br.com.marealta.domain.RoomCatalog;
import br.com.marealta.notification.FinanceSubscriber;
import br.com.marealta.notification.GuestSubscriber;
import br.com.marealta.notification.HousekeepingSubscriber;
import br.com.marealta.notification.Notifier;
import br.com.marealta.notification.Subscriber;
import br.com.marealta.persistence.Database;
import br.com.marealta.persistence.JdbcMessageRepository;
import br.com.marealta.persistence.JdbcReservationRepository;
import br.com.marealta.report.OccupancyReport;
import br.com.marealta.report.RevenueReport;
import br.com.marealta.web.WebApp;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        var dataSource = Database.open(requireEnv("DB_URL"), requireEnv("DB_USER"), requireEnv("DB_PASSWORD"));
        var rooms = new RoomCatalog();
        var reservations = new JdbcReservationRepository(dataSource);
        var messages = new JdbcMessageRepository(dataSource);

        // Quem é avisado dos eventos de uma reserva: para incluir (restaurante, transfer...) ou tirar alguém,
        // basta escrever a classe do interessado e mexer nesta lista.
        List<Subscriber> subscribers = List.of(
                new GuestSubscriber(rooms),
                new HousekeepingSubscriber(),
                new FinanceSubscriber());

        var service = new ReservationService(rooms, reservations, new Notifier(subscribers, messages));
        var web = new WebApp(rooms, service, messages,
                new OccupancyReport(rooms, reservations), new RevenueReport(reservations));
        web.create().start(Integer.parseInt(System.getenv().getOrDefault("PORT", "7070")));
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente " + name + " não definida");
        }
        return value;
    }
}

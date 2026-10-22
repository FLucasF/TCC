package br.com.marealta;

import br.com.marealta.event.Subscriber;
import br.com.marealta.notify.FinanceNotifier;
import br.com.marealta.notify.GuestNotifier;
import br.com.marealta.notify.HousekeepingNotifier;
import br.com.marealta.service.ReportService;
import br.com.marealta.service.ReservationService;
import br.com.marealta.store.JdbcStore;
import br.com.marealta.web.Api;

import java.util.List;

public class Main {
    /** Quem é avisado dos eventos. Restaurante, transfer etc. entram (e saem) aqui. */
    static List<Subscriber> subscribers() {
        return List.of(new GuestNotifier(), new HousekeepingNotifier(), new FinanceNotifier());
    }

    public static void main(String[] args) {
        String port = System.getenv("PORT");
        JdbcStore store = new JdbcStore(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        ReservationService reservations = new ReservationService(store, subscribers());
        new Api(reservations, new ReportService(store)).build().start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port));
    }
}

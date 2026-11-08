package br.com.pousadamarealta.reservas;

import io.javalin.Javalin;
import br.com.pousadamarealta.reservas.domain.Room;
import br.com.pousadamarealta.reservas.events.EventBus;
import br.com.pousadamarealta.reservas.notification.FinanceNotifier;
import br.com.pousadamarealta.reservas.notification.GovernanceNotifier;
import br.com.pousadamarealta.reservas.notification.GuestNotifier;
import br.com.pousadamarealta.reservas.persistence.Database;
import br.com.pousadamarealta.reservas.persistence.MessageRepository;
import br.com.pousadamarealta.reservas.persistence.ReservationRepository;
import br.com.pousadamarealta.reservas.persistence.RoomRepository;
import br.com.pousadamarealta.reservas.persistence.Schema;
import br.com.pousadamarealta.reservas.service.ReportService;
import br.com.pousadamarealta.reservas.service.ReservationService;
import br.com.pousadamarealta.reservas.web.Routes;

/** Monta o sistema. */
public final class App {
    private App() {}

    public static Javalin create(Database db) {
        Schema.create(db);

        RoomRepository rooms = new RoomRepository(db);
        ReservationRepository reservations = new ReservationRepository(db);
        MessageRepository messages = new MessageRepository(db);

        // Quem é avisado dos eventos da reserva. Para incluir (ou tirar) alguém, como o restaurante ou o
        // transfer, basta escrever um EventSubscriber e registrá-lo aqui; o resto do sistema não muda.
        EventBus events = new EventBus();
        events.subscribe(new GuestNotifier(messages));
        events.subscribe(new GovernanceNotifier(messages));
        events.subscribe(new FinanceNotifier(messages));

        ReservationService reservationService = new ReservationService(db, rooms, reservations, events);
        ReportService reportService = new ReportService(db, rooms, reservations);

        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);
        new Routes(reservationService, reportService, messages, db).register(app);
        return app;
    }
}

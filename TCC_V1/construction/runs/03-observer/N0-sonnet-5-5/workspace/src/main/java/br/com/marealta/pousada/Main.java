package br.com.marealta.pousada;

import br.com.marealta.pousada.events.*;
import br.com.marealta.pousada.web.Api;
import io.javalin.Javalin;

public class Main {
    /** Wires the application. New interested parties (restaurant, transfer...) are just more subscribers here. */
    public static Javalin build() {
        Outbox outbox = new Outbox();
        EventBus bus = new EventBus();
        bus.subscribe(new GuestConfirmationNotifier(outbox));
        bus.subscribe(new HousekeepingNotifier(outbox));
        bus.subscribe(new FinanceNotifier(outbox));
        ReservationService service = new ReservationService(ReservationService.defaultRooms(), bus);
        return new Api(service, outbox).create();
    }

    public static void main(String[] args) {
        String port = System.getenv("PORT");
        build().start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }
}

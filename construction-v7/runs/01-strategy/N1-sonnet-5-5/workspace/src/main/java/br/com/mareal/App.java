package br.com.mareal;

import br.com.mareal.api.Routes;
import br.com.mareal.domain.Outbox;
import br.com.mareal.domain.ReservationService;
import java.time.Clock;

public class App {
    public static void main(String[] args) {
        String port = System.getenv("PORT");
        Outbox outbox = new Outbox();
        Routes.create(new ReservationService(outbox, Clock.systemDefaultZone()), outbox)
                .start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }
}

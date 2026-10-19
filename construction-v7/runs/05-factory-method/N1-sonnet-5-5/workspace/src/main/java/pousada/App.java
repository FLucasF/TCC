package pousada;

import io.javalin.Javalin;
import pousada.domain.Outbox;
import pousada.domain.ReservationService;
import pousada.domain.Room;
import pousada.web.Api;

public final class App {

    private App() {
    }

    public static Javalin create() {
        Outbox outbox = new Outbox();
        return Api.create(new ReservationService(Room.inn(), outbox), outbox);
    }

    public static void main(String[] args) {
        String port = System.getenv("PORT");
        create().start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }
}

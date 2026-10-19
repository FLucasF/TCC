package pousada;

import pousada.messaging.Message;
import pousada.messaging.Outbox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ReservationService {
    public static final List<Room> ROOMS = List.of(
            new Room(101, RoomType.STANDARD),
            new Room(102, RoomType.STANDARD),
            new Room(201, RoomType.DELUXE),
            new Room(202, RoomType.DELUXE),
            new Room(301, RoomType.SUITE));

    private final Outbox outbox;
    private final List<Reservation> reservations = new ArrayList<>();

    public ReservationService(Outbox outbox) {
        this.outbox = outbox;
    }

    public List<RoomView> rooms() {
        return ROOMS.stream().map(RoomView::of).toList();
    }

    public synchronized ReservationView create(BookingRequest request) {
        Room room = ROOMS.stream().filter(r -> r.number() == request.roomNumber()).findFirst()
                .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Quarto inexistente"));
        if (request.guests() > room.type().capacity) {
            throw new ApiException(422, "CAPACITY_EXCEEDED", "O quarto comporta até " + room.type().capacity + " hóspedes");
        }
        boolean taken = reservations.stream().anyMatch(r -> r.room.equals(room)
                && r.status() != ReservationStatus.CANCELLED
                && r.overlaps(request.checkIn(), request.checkOut()));
        if (taken) {
            throw new ApiException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites");
        }
        Reservation reservation = new Reservation(reservations.size() + 1, room, request.guestName(),
                request.guestEmail(), request.guestCpf(), request.checkIn(), request.checkOut(),
                request.guests(), request.ratePlan());
        reservations.add(reservation);
        return ReservationView.of(reservation);
    }

    public synchronized ReservationView get(String id) {
        return ReservationView.of(find(id));
    }

    public synchronized ReservationView confirm(String id) {
        Reservation r = find(id);
        r.confirm();
        outbox.send(confirmationEmail(r));
        return ReservationView.of(r);
    }

    public synchronized ReservationView cancel(String id) {
        Reservation r = find(id);
        r.cancel();
        return ReservationView.of(r);
    }

    public synchronized ReservationView checkIn(String id) {
        Reservation r = find(id);
        r.checkIn();
        return ReservationView.of(r);
    }

    public synchronized Receipt checkOut(String id) {
        Reservation r = find(id);
        r.checkOut();
        return Receipt.of(r);
    }

    /** Segunda via do comprovante, só para reserva paga. */
    public synchronized Receipt receipt(String id) {
        Reservation r = find(id);
        if (!r.status().isPaid()) {
            throw ApiException.notAllowed("Só existe comprovante para reserva paga");
        }
        return Receipt.of(r);
    }

    /** Executa uma leitura sobre todas as reservas, sem que mudem durante ela. */
    public synchronized <T> T read(Function<List<Reservation>, T> reader) {
        return reader.apply(List.copyOf(reservations));
    }

    private Reservation find(String id) {
        try {
            int number = Integer.parseInt(id);
            if (number >= 1 && number <= reservations.size()) {
                return reservations.get(number - 1);
            }
        } catch (NumberFormatException ignored) {
            // cai no erro abaixo
        }
        throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva inexistente");
    }

    private static Message confirmationEmail(Reservation r) {
        String body = "Olá, " + r.guestName + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Receipt.text(r) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return Message.email(r.guestEmail, "Reserva confirmada - Pousada Maré Alta", body);
    }
}

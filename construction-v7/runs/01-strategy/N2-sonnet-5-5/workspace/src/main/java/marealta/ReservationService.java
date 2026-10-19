package marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {
    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan plan,
                                 String companyCode) {}

    private final List<Room> rooms = List.of(
            new Room(101, RoomType.STANDARD),
            new Room(102, RoomType.STANDARD),
            new Room(201, RoomType.DELUXE),
            new Room(202, RoomType.DELUXE),
            new Room(301, RoomType.SUITE));
    private final List<Reservation> reservations = new ArrayList<>();
    private final Outbox outbox;

    public ReservationService(Outbox outbox) {
        this.outbox = outbox;
    }

    public List<Room> rooms() { return rooms; }

    public synchronized List<Reservation> reservations() { return List.copyOf(reservations); }

    public synchronized Reservation create(NewReservation req) {
        Room room = rooms.stream().filter(r -> r.number() == req.roomNumber()).findFirst()
                .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Quarto " + req.roomNumber() + " não existe"));

        Stay stay = new Stay(room, req.checkIn(), req.checkOut(), req.companyCode());
        req.plan().validate(stay);

        if (req.guests() > room.capacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED",
                    "O quarto " + room.number() + " comporta até " + room.capacity() + " pessoas");
        }
        boolean taken = reservations.stream().anyMatch(r ->
                r.isActive() && r.room().equals(room) && r.overlaps(req.checkIn(), req.checkOut()));
        if (taken) {
            throw new ApiException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites");
        }

        BigDecimal nightsTotal = Money.ZERO;
        for (LocalDate night = req.checkIn(); night.isBefore(req.checkOut()); night = night.plusDays(1)) {
            nightsTotal = nightsTotal.add(room.priceOn(night));
        }
        Reservation reservation = new Reservation(reservations.size() + 1, room, req.guestName(), req.guestEmail(),
                req.guestCpf(), req.checkIn(), req.checkOut(), req.guests(), req.plan(),
                req.plan().quote(stay, Money.round(nightsTotal)));
        reservations.add(reservation);
        return reservation;
    }

    public synchronized Reservation get(int id) {
        if (id < 1 || id > reservations.size()) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe");
        }
        return reservations.get(id - 1);
    }

    public synchronized Reservation confirm(int id) {
        Reservation r = get(id);
        r.confirm();
        String receipt = Receipt.of(r).toText();
        outbox.sendEmail(r.guestEmail(), "Reserva confirmada - Pousada Maré Alta",
                "Olá, " + r.guestName() + "!\n\n"
                        + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                        + receipt + "\n\n"
                        + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                        + "Até breve!");
        return r;
    }

    public synchronized Reservation cancel(int id) {
        Reservation r = get(id);
        r.cancel();
        return r;
    }

    public synchronized Reservation checkIn(int id) {
        Reservation r = get(id);
        r.checkIn();
        return r;
    }

    public synchronized Receipt checkOut(int id) {
        Reservation r = get(id);
        r.checkOut();
        return Receipt.of(r);
    }

    public synchronized Receipt document(int id) {
        Reservation r = get(id);
        if (!r.isPaid()) {
            throw ApiException.notAllowed("Segunda via só existe para reserva paga");
        }
        return Receipt.of(r);
    }
}

package pousada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReservationService {
    static final String SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private final Store store;

    public ReservationService(Store store) {
        this.store = store;
    }

    public record NewReservation(int roomNumber, String guestName, String guestEmail, Identification identification,
            LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {}

    public Reservation create(NewReservation n) {
        Room room = Room.find(n.roomNumber())
                .orElseThrow(() -> new ApiException(404, "ROOM_NOT_FOUND", "Quarto não encontrado: " + n.roomNumber()));
        if (n.guests() > room.capacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED",
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        Pricing.Quote q = Pricing.quote(room.type(), n.checkIn(), n.checkOut(), n.ratePlan());
        return store.tx(c -> {
            store.lockRoom(c, room.number());
            if (store.isRoomBusy(c, room.number(), n.checkIn(), n.checkOut())) {
                throw new ApiException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites");
            }
            return store.insert(c, new Reservation(0, room.number(), n.guestName(), n.guestEmail(),
                    n.identification(), n.checkIn(), n.checkOut(), n.guests(), n.ratePlan(), q.nights(),
                    q.subtotal(), q.discount(), q.total(), Status.PENDING, Money.ZERO, null));
        });
    }

    public Reservation get(long id) {
        return store.tx(c -> store.find(c, id, false).orElseThrow(ReservationService::notFound));
    }

    public Reservation confirm(long id) {
        return store.tx(c -> {
            Reservation r = load(c, id);
            require(r.status() == Status.PENDING, "Só é possível confirmar uma reserva pendente");
            store.updateState(c, id, Status.CONFIRMED, r.total(), null);
            Reservation paid = load(c, id);
            store.addMessage(c, new Message(0, "EMAIL", paid.guestEmail(), SUBJECT, confirmationBody(paid)));
            return paid;
        });
    }

    public Reservation checkIn(long id) {
        return transition(id, Status.CONFIRMED, Status.CHECKED_IN, "Só é possível fazer check-in de uma reserva confirmada");
    }

    public Reservation checkOut(long id) {
        return transition(id, Status.CHECKED_IN, Status.CHECKED_OUT,
                "Só é possível fazer check-out de uma reserva com check-in");
    }

    public Reservation cancel(long id) {
        return store.tx(c -> {
            Reservation r = load(c, id);
            require(r.status() == Status.PENDING || r.status() == Status.CONFIRMED,
                    "Só é possível cancelar uma reserva pendente ou confirmada");
            BigDecimal refund = r.ratePlan() == RatePlan.STANDARD ? r.amountPaid() : Money.ZERO;
            store.updateState(c, id, Status.CANCELLED, r.amountPaid(), refund);
            return load(c, id);
        });
    }

    /** Segunda via: só existe para reserva paga. */
    public Document document(long id) {
        Reservation r = get(id);
        require(r.status().isPaid(), "Só há documento para reserva paga");
        return Document.of(r);
    }

    public List<Message> messages() {
        return store.tx(store::messages);
    }

    private Reservation transition(long id, Status from, Status to, String error) {
        return store.tx(c -> {
            Reservation r = load(c, id);
            require(r.status() == from, error);
            store.updateState(c, id, to, r.amountPaid(), r.refund());
            return load(c, id);
        });
    }

    private Reservation load(java.sql.Connection c, long id) throws java.sql.SQLException {
        return store.find(c, id, true).orElseThrow(ReservationService::notFound);
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw ApiException.notAllowed(message);
    }

    private static ApiException notFound() {
        return new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
    }

    static String confirmationBody(Reservation r) {
        return "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + r.identification().render(r) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}

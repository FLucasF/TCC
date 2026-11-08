package br.com.mare.domain;

import br.com.mare.infra.Db;
import br.com.mare.infra.MessageRepository;
import br.com.mare.infra.ReservationRepository;
import br.com.mare.plan.Booking;
import br.com.mare.plan.Quote;
import br.com.mare.plan.RatePlan;
import br.com.mare.plan.RatePlans;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.function.Consumer;

public class ReservationService {
    static final String CONFIRMATION_SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private final Db db;
    private final ReservationRepository reservations;
    private final MessageRepository messages;

    public ReservationService(Db db, ReservationRepository reservations, MessageRepository messages) {
        this.db = db;
        this.reservations = reservations;
        this.messages = messages;
    }

    public Reservation create(ReservationRequest req) {
        Room room = Rooms.find(req.roomNumber())
                .orElseThrow(() -> ApiException.roomNotFound("Quarto " + req.roomNumber() + " não existe"));
        if (req.guests() > room.capacity()) {
            throw ApiException.capacityExceeded("O quarto " + room.number() + " comporta até " + room.capacity() + " pessoas");
        }
        RatePlan plan = RatePlans.find(req.ratePlan())
                .orElseThrow(() -> ApiException.invalid("Plano de tarifa desconhecido: " + req.ratePlan()));
        Quote quote = plan.quote(new Booking(room, req.checkIn(), req.checkOut(), req.companyCode()));

        return db.transaction(c -> {
            try (Statement s = c.createStatement()) {
                s.execute("SELECT pg_advisory_xact_lock(2)");
            }
            if (reservations.hasConflict(c, room.number(), req.checkIn(), req.checkOut())) {
                throw ApiException.roomUnavailable("O quarto " + room.number() + " já está reservado em alguma das noites");
            }
            Reservation draft = new Reservation(0, room, req.guestName(), req.guestEmail(), req.guestCpf(),
                    req.checkIn(), req.checkOut(), req.guests(), plan, quote.nights(), quote.subtotal(),
                    quote.discount(), quote.total(), Status.PENDING, Money.ZERO, null);
            int id = reservations.insert(c, draft);
            return reservations.find(c, id, false).orElseThrow();
        });
    }

    public Reservation get(int id) {
        return db.transaction(c -> load(c, id, false));
    }

    public Reservation confirm(int id) {
        return change(id, Reservation::confirm, (c, r) -> {
            String body = "Olá, " + r.guestName() + "!\n\n"
                    + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                    + Receipt.of(r).text() + "\n\n"
                    + "A entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!";
            messages.save(c, Message.EMAIL, r.guestEmail(), CONFIRMATION_SUBJECT, body);
        });
    }

    public Reservation cancel(int id) { return change(id, Reservation::cancel, (c, r) -> {}); }

    public Reservation checkIn(int id) { return change(id, Reservation::checkIn, (c, r) -> {}); }

    public Receipt checkOut(int id) {
        return Receipt.of(change(id, Reservation::checkOut, (c, r) -> {}));
    }

    public Receipt document(int id) {
        Reservation r = get(id);
        r.requirePaid();
        return Receipt.of(r);
    }

    public List<Message> messages() {
        return db.transaction(messages::findAll);
    }

    private interface SideEffect {
        void run(Connection c, Reservation r) throws SQLException;
    }

    private Reservation change(int id, Consumer<Reservation> transition, SideEffect afterwards) {
        return db.transaction(c -> {
            Reservation r = load(c, id, true);
            transition.accept(r);
            reservations.updateState(c, r);
            afterwards.run(c, r);
            return r;
        });
    }

    private Reservation load(Connection c, int id, boolean lock) throws SQLException {
        return reservations.find(c, id, lock)
                .orElseThrow(() -> ApiException.reservationNotFound("Reserva " + id + " não existe"));
    }
}

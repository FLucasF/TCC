package br.com.marealta.service;

import br.com.marealta.DomainException;
import br.com.marealta.domain.Message;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.Rooms;
import br.com.marealta.domain.Stay;
import br.com.marealta.infra.Database;
import br.com.marealta.infra.MessageRepository;
import br.com.marealta.infra.ReservationRepository;
import br.com.marealta.plan.Quote;
import br.com.marealta.plan.RatePlan;
import br.com.marealta.plan.RatePlans;

import java.util.List;
import java.util.function.UnaryOperator;

public class ReservationService {
    private final Database db;
    private final ReservationRepository reservations;
    private final MessageRepository messages;
    private final RatePlans plans;

    public ReservationService(Database db, ReservationRepository reservations, MessageRepository messages,
                              RatePlans plans) {
        this.db = db;
        this.reservations = reservations;
        this.messages = messages;
        this.plans = plans;
    }

    public Reservation create(ReservationRequest req) {
        Room room = Rooms.find(req.roomNumber())
                .orElseThrow(() -> new DomainException(404, "ROOM_NOT_FOUND", "Quarto não encontrado"));
        if (req.guests() > room.capacity()) {
            throw new DomainException(422, "CAPACITY_EXCEEDED", "O quarto comporta até " + room.capacity() + " pessoas");
        }
        RatePlan plan = plans.get(req.ratePlan());
        Quote quote = plan.quote(new Stay(room, req.checkIn(), req.checkOut(), req.companyCode()));
        Reservation draft = new Reservation(0, room.number(), req.guestName(), req.guestEmail(), req.guestCpf(),
                req.checkIn(), req.checkOut(), req.guests(), plan.code(), quote.nights(), quote.subtotal(),
                quote.discount(), quote.total(), ReservationStatus.PENDING, Money.ZERO, null);
        return db.inTransaction(c -> {
            reservations.lockRoom(c, room.number());
            if (reservations.hasConflict(c, room.number(), req.checkIn(), req.checkOut())) {
                throw new DomainException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites");
            }
            return reservations.insert(c, draft);
        });
    }

    public Reservation get(long id) {
        return db.inTransaction(c -> reservations.find(c, id, false).orElseThrow(ReservationService::notFound));
    }

    public Reservation confirm(long id) {
        return db.inTransaction(c -> {
            Reservation confirmed = reservations.find(c, id, true).orElseThrow(ReservationService::notFound).confirm();
            reservations.update(c, confirmed);
            messages.save(c, ConfirmationEmail.of(confirmed, Rooms.find(confirmed.roomNumber()).orElseThrow()));
            return confirmed;
        });
    }

    public Reservation cancel(long id) {
        return transition(id, r -> r.cancel(plans.get(r.ratePlan())));
    }

    public Reservation checkIn(long id) {
        return transition(id, Reservation::startStay);
    }

    public Receipt checkOut(long id) {
        return receiptOf(transition(id, Reservation::endStay));
    }

    public Receipt document(long id) {
        Reservation r = get(id);
        if (!r.status().isPaid()) {
            throw DomainException.notAllowed("A segunda via só existe para reserva paga");
        }
        return receiptOf(r);
    }

    public List<Message> messages() {
        return db.inTransaction(messages::findAll);
    }

    private Reservation transition(long id, UnaryOperator<Reservation> change) {
        return db.inTransaction(c -> {
            Reservation updated = change.apply(reservations.find(c, id, true).orElseThrow(ReservationService::notFound));
            reservations.update(c, updated);
            return updated;
        });
    }

    private static Receipt receiptOf(Reservation r) {
        return Receipt.of(r, Rooms.find(r.roomNumber()).orElseThrow());
    }

    private static DomainException notFound() {
        return new DomainException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
    }
}

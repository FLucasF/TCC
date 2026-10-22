package br.com.marealta.service;

import br.com.marealta.domain.AppException;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.Pricing;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;
import br.com.marealta.event.DomainEvent;
import br.com.marealta.event.Message;
import br.com.marealta.event.Subscriber;
import br.com.marealta.store.Store;
import br.com.marealta.store.Store.Tx;

import java.time.LocalDate;
import java.util.List;

import static br.com.marealta.domain.ReservationStatus.*;

public class ReservationService {
    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {}

    private final Store store;
    private final List<Subscriber> subscribers;

    public ReservationService(Store store, List<Subscriber> subscribers) {
        this.store = store;
        this.subscribers = List.copyOf(subscribers);
    }

    public List<Room> rooms() {
        return store.tx(Tx::rooms);
    }

    public List<Message> messages() {
        return store.tx(Tx::messages);
    }

    public Reservation create(NewReservation n) {
        if (n.guests() < 1 || !n.checkOut().isAfter(n.checkIn())) {
            throw AppException.invalid("Dados da reserva inválidos");
        }
        return store.tx(tx -> {
            Room room = tx.room(n.roomNumber()).orElseThrow(() -> AppException.roomNotFound(n.roomNumber()));
            if (n.guests() > room.capacity()) throw AppException.capacity(room.capacity());
            tx.lockRoom(room.number());
            if (tx.hasOverlap(room.number(), n.checkIn(), n.checkOut())) throw AppException.unavailable();
            Pricing.Quote q = Pricing.quote(room, n.checkIn(), n.checkOut(), n.ratePlan());
            return tx.insert(new Reservation(0, room.number(), n.guestName(), n.guestEmail(), n.guestCpf(),
                    n.checkIn(), n.checkOut(), n.guests(), n.ratePlan(), q.nights(), q.subtotal(), q.discount(),
                    q.total(), PENDING, Money.ZERO, null));
        });
    }

    public Reservation get(String id) {
        return store.tx(tx -> load(tx, id, false));
    }

    public Reservation confirm(String id) {
        return store.tx(tx -> {
            Reservation r = load(tx, id, true);
            require(r, PENDING, "Só é possível confirmar uma reserva pendente");
            Reservation paid = r.withStatus(CONFIRMED).withPayment(r.total());
            tx.update(paid);
            publish(tx, new DomainEvent.ReservationConfirmed(paid, room(tx, paid)));
            return paid;
        });
    }

    public Reservation cancel(String id) {
        return store.tx(tx -> {
            Reservation r = load(tx, id, true);
            if (r.status() != PENDING && r.status() != CONFIRMED) {
                throw AppException.notAllowed("Só é possível cancelar antes do check-in");
            }
            boolean wasConfirmed = r.status() == CONFIRMED;
            var refund = wasConfirmed && r.ratePlan() == RatePlan.STANDARD ? r.amountPaid() : Money.ZERO;
            Reservation cancelled = r.withStatus(CANCELLED).withRefund(refund);
            tx.update(cancelled);
            publish(tx, new DomainEvent.ReservationCancelled(cancelled, room(tx, cancelled), wasConfirmed, refund));
            return cancelled;
        });
    }

    public Reservation checkIn(String id) {
        return store.tx(tx -> {
            Reservation r = load(tx, id, true);
            require(r, CONFIRMED, "Só é possível fazer check-in de uma reserva confirmada");
            Reservation next = r.withStatus(CHECKED_IN);
            tx.update(next);
            return next;
        });
    }

    public Receipt checkOut(String id) {
        return store.tx(tx -> {
            Reservation r = load(tx, id, true);
            require(r, CHECKED_IN, "Só é possível fazer check-out de quem fez check-in");
            Reservation next = r.withStatus(CHECKED_OUT);
            tx.update(next);
            return Receipt.of(next, room(tx, next));
        });
    }

    /** Segunda via do comprovante; só existe para reserva paga. */
    public Receipt document(String id) {
        return store.tx(tx -> {
            Reservation r = load(tx, id, false);
            if (!r.status().isPaid()) {
                throw AppException.notAllowed("Comprovante só existe para reserva paga");
            }
            return Receipt.of(r, room(tx, r));
        });
    }

    private void publish(Tx tx, DomainEvent event) {
        for (Subscriber s : subscribers) {
            for (Message m : s.on(event)) tx.addMessage(m);
        }
    }

    private static void require(Reservation r, ReservationStatus expected, String message) {
        if (r.status() != expected) throw AppException.notAllowed(message);
    }

    private static Room room(Tx tx, Reservation r) {
        return tx.room(r.roomNumber()).orElseThrow();
    }

    private static Reservation load(Tx tx, String id, boolean lock) {
        long n;
        try {
            n = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw AppException.reservationNotFound(id);
        }
        return (lock ? tx.findForUpdate(n) : tx.find(n)).orElseThrow(() -> AppException.reservationNotFound(id));
    }
}

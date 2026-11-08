package br.com.marealta.service;

import br.com.marealta.domain.*;
import br.com.marealta.store.Store;
import br.com.marealta.store.Tx;

import java.util.List;
import java.util.function.Function;

public final class ReservationService {
    private final Store store;

    public ReservationService(Store store) {
        this.store = store;
    }

    public List<Room> rooms() {
        return store.inTransaction(Tx::rooms);
    }

    public List<Message> messages() {
        return store.inTransaction(Tx::messages);
    }

    public Reservation create(NewReservation cmd) {
        return store.inTransaction(tx -> {
            Room room = tx.room(cmd.roomNumber()).orElseThrow(() ->
                    new PousadaException(ErrorCode.ROOM_NOT_FOUND, "Room " + cmd.roomNumber() + " not found"));
            if (cmd.guests() > room.capacity()) {
                throw new PousadaException(ErrorCode.CAPACITY_EXCEEDED,
                        "Room " + room.number() + " holds at most " + room.capacity() + " guests");
            }
            tx.lockRoom(room.number());
            if (tx.roomBooked(room.number(), cmd.checkIn(), cmd.checkOut())) {
                throw new PousadaException(ErrorCode.ROOM_UNAVAILABLE,
                        "Room " + room.number() + " is already booked in the requested period");
            }
            Quote quote = Quote.of(room, cmd.checkIn(), cmd.checkOut(), cmd.ratePlan());
            return tx.insert(new Reservation(0, room.number(), cmd.guestName(), cmd.guestEmail(), cmd.guestCpf(),
                    cmd.checkIn(), cmd.checkOut(), cmd.guests(), cmd.ratePlan(), quote.nights(), quote.subtotal(),
                    quote.discount(), quote.total(), ReservationStatus.PENDING, Money.ZERO, null));
        });
    }

    public Reservation get(long id) {
        return store.inTransaction(tx -> tx.reservation(id).orElseThrow(() -> notFound(id)));
    }

    /** Payment received: confirms the reservation and queues the confirmation e-mail. */
    public Reservation confirm(long id) {
        return store.inTransaction(tx -> {
            Reservation confirmed = transition(tx, id, Reservation::confirm);
            Receipt receipt = Receipt.of(confirmed, room(tx, confirmed));
            tx.saveMessage(ConfirmationEmail.compose(confirmed, receipt));
            return confirmed;
        });
    }

    public Reservation cancel(long id) {
        return store.inTransaction(tx -> transition(tx, id, Reservation::cancel));
    }

    public Reservation checkIn(long id) {
        return store.inTransaction(tx -> transition(tx, id, Reservation::startStay));
    }

    public Receipt checkOut(long id) {
        return store.inTransaction(tx -> {
            Reservation done = transition(tx, id, Reservation::endStay);
            return Receipt.of(done, room(tx, done));
        });
    }

    /** Second copy of the receipt; only exists for paid reservations. */
    public Receipt document(long id) {
        return store.inTransaction(tx -> {
            Reservation r = tx.reservation(id).orElseThrow(() -> notFound(id));
            if (!r.paid()) {
                throw new PousadaException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "Receipt only exists for paid reservations");
            }
            return Receipt.of(r, room(tx, r));
        });
    }

    private Reservation transition(Tx tx, long id, Function<Reservation, Reservation> change) {
        Reservation current = tx.reservationForUpdate(id).orElseThrow(() -> notFound(id));
        Reservation next = change.apply(current);
        tx.update(next);
        return next;
    }

    private Room room(Tx tx, Reservation r) {
        return tx.room(r.roomNumber()).orElseThrow(() ->
                new IllegalStateException("Room " + r.roomNumber() + " missing"));
    }

    private PousadaException notFound(long id) {
        return new PousadaException(ErrorCode.RESERVATION_NOT_FOUND, "Reservation " + id + " not found");
    }
}

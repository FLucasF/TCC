package br.com.marealta;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

public class ReservationService {
    private final Database db;
    private final Rooms rooms;
    private final ReservationRepository reservations;
    private final MessageRepository messages;
    private final List<ReservationListener> listeners;

    public ReservationService(Database db, Rooms rooms, ReservationRepository reservations,
                              MessageRepository messages, List<ReservationListener> listeners) {
        this.db = db;
        this.rooms = rooms;
        this.reservations = reservations;
        this.messages = messages;
        this.listeners = List.copyOf(listeners);
    }

    public Reservation create(NewReservation request) {
        Room room = rooms.get(request.roomNumber());
        if (request.guests() > room.capacity()) {
            throw DomainException.capacityExceeded(room.number(), room.capacity());
        }
        Pricing.Quote quote = Pricing.quote(room, request.checkIn(), request.checkOut(), request.ratePlan());
        return db.inTransaction(c -> {
            reservations.lockRoom(c, room.number());
            if (reservations.isRoomBooked(c, room.number(), request.checkIn(), request.checkOut())) {
                throw DomainException.roomUnavailable(room.number());
            }
            return reservations.insert(c, new Reservation(0, room.number(), request.guestName(),
                    request.guestEmail(), request.guestCpf(), request.checkIn(), request.checkOut(),
                    request.guests(), request.ratePlan(), quote.nights(), quote.subtotal(), quote.discount(),
                    quote.total(), ReservationStatus.PENDING, Money.ZERO, null));
        });
    }

    public Reservation get(String id) {
        return db.inTransaction(c -> reservations.find(c, parseId(id))
                .orElseThrow(() -> DomainException.reservationNotFound(id)));
    }

    public Reservation confirm(String id) {
        return change(id, Reservation::confirm, (before, after) -> notify(after, ReservationListener::confirmed));
    }

    public Reservation cancel(String id) {
        return change(id, Reservation::cancel, (before, after) -> before.status() == ReservationStatus.CONFIRMED
                ? notify(after, ReservationListener::cancelledAfterConfirmation)
                : List.of());
    }

    public Reservation checkIn(String id) {
        return change(id, Reservation::checkInGuest, (before, after) -> List.of());
    }

    public Receipt checkOut(String id) {
        Reservation r = change(id, Reservation::checkOutGuest, (before, after) -> List.of());
        return Receipt.of(r, rooms.get(r.roomNumber()));
    }

    /** Segunda via do comprovante: só para reserva paga. */
    public Receipt document(String id) {
        Reservation r = get(id);
        if (!r.status().isPaid()) {
            throw DomainException.notAllowed("O comprovante só existe para reserva paga");
        }
        return Receipt.of(r, rooms.get(r.roomNumber()));
    }

    public List<Message> sentMessages() {
        return db.inTransaction(messages::findAll);
    }

    /**
     * Mudança de situação: trava a reserva, aplica a transição, grava e guarda as mensagens
     * resultantes, tudo na mesma transação.
     */
    private Reservation change(String id, UnaryOperator<Reservation> transition,
                               BiFunction<Reservation, Reservation, List<Message>> notifications) {
        long reservationId = parseId(id);
        return db.inTransaction(c -> {
            Reservation before = reservations.findForUpdate(c, reservationId)
                    .orElseThrow(() -> DomainException.reservationNotFound(id));
            Reservation after = transition.apply(before);
            reservations.update(c, after);
            messages.saveAll(c, notifications.apply(before, after));
            return after;
        });
    }

    private List<Message> notify(Reservation r, BiFunction<ReservationListener, Reservation, List<Message>> event) {
        List<Message> result = new ArrayList<>();
        for (ReservationListener listener : listeners) result.addAll(event.apply(listener, r));
        return result;
    }

    private long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw DomainException.reservationNotFound(id);
        }
    }
}

package br.com.marealta.pousada.service;

import br.com.marealta.pousada.domain.AppException;
import br.com.marealta.pousada.domain.ErrorCode;
import br.com.marealta.pousada.domain.Message;
import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.domain.Receipt;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationStatus;
import br.com.marealta.pousada.domain.Room;
import br.com.marealta.pousada.events.NotificationRule;
import br.com.marealta.pousada.events.ReservationEvent;
import br.com.marealta.pousada.persistence.Database;
import br.com.marealta.pousada.persistence.Tx;
import java.time.LocalDate;
import java.util.List;
import java.util.function.UnaryOperator;

public class ReservationService {

    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
    }

    private final Database db;
    private final List<NotificationRule> rules;

    public ReservationService(Database db, List<NotificationRule> rules) {
        this.db = db;
        this.rules = List.copyOf(rules);
    }

    public List<Room> rooms() {
        return db.inTransaction(tx -> tx.rooms().findAll());
    }

    public List<Message> messages() {
        return db.inTransaction(tx -> tx.messages().findAll());
    }

    public Reservation get(long id) {
        return db.inTransaction(tx -> tx.reservations().find(id).orElseThrow(() -> notFound(id)));
    }

    public Reservation create(NewReservation n) {
        return db.inTransaction(tx -> {
            Room room = tx.rooms().findForUpdate(n.roomNumber())
                    .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND, "Quarto " + n.roomNumber() + " não existe"));
            if (n.guests() > room.capacity()) {
                throw new AppException(ErrorCode.CAPACITY_EXCEEDED,
                        "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
            }
            if (tx.reservations().hasOverlap(room.number(), n.checkIn(), n.checkOut())) {
                throw new AppException(ErrorCode.ROOM_UNAVAILABLE,
                        "O quarto " + room.number() + " já está reservado em alguma das noites pedidas");
            }
            return tx.reservations().insert(Reservation.pending(room, n.guestName(), n.guestEmail(), n.guestCpf(),
                    n.checkIn(), n.checkOut(), n.guests(), n.ratePlan()));
        });
    }

    public Reservation confirm(long id) {
        return transition(id, Reservation::confirm);
    }

    public Reservation cancel(long id) {
        return transition(id, Reservation::cancel);
    }

    public Reservation checkIn(long id) {
        return transition(id, Reservation::startStay);
    }

    public Receipt checkOut(long id) {
        return db.inTransaction(tx -> {
            Reservation r = lock(tx, id).endStay();
            tx.reservations().update(r);
            return Receipt.of(r, room(tx, r));
        });
    }

    /** Segunda via do comprovante; só existe para reserva paga. */
    public Receipt document(long id) {
        return db.inTransaction(tx -> {
            Reservation r = tx.reservations().find(id).orElseThrow(() -> notFound(id));
            if (!r.paid()) {
                throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "A reserva " + id + " está " + r.status() + " e não tem comprovante");
            }
            return Receipt.of(r, room(tx, r));
        });
    }

    private Reservation transition(long id, UnaryOperator<Reservation> change) {
        return db.inTransaction(tx -> {
            Reservation before = lock(tx, id);
            Reservation after = change.apply(before);
            tx.reservations().update(after);
            Room room = room(tx, after);
            ReservationEvent event = switch (after.status()) {
                case CONFIRMED -> new ReservationEvent.Confirmed(after, room);
                case CANCELLED -> new ReservationEvent.Cancelled(after, room,
                        before.status() == ReservationStatus.CONFIRMED);
                default -> null;
            };
            if (event != null) {
                publish(tx, event);
            }
            return after;
        });
    }

    private void publish(Tx tx, ReservationEvent event) {
        for (NotificationRule rule : rules) {
            rule.messagesFor(event).forEach(tx.messages()::add);
        }
    }

    private static Reservation lock(Tx tx, long id) {
        return tx.reservations().findForUpdate(id).orElseThrow(() -> notFound(id));
    }

    private static Room room(Tx tx, Reservation r) {
        return tx.rooms().find(r.roomNumber()).orElseThrow();
    }

    private static AppException notFound(long id) {
        return new AppException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + id + " não existe");
    }
}

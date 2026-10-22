package pousada.service;

import pousada.document.IssuedDocument;
import pousada.domain.Channel;
import pousada.domain.ErrorCode;
import pousada.domain.Message;
import pousada.domain.PousadaException;
import pousada.domain.Quote;
import pousada.domain.Reservation;
import pousada.domain.Room;
import pousada.domain.RoomCatalog;
import pousada.persistence.Database;
import pousada.persistence.MessageRepository;
import pousada.persistence.ReservationRepository;

import java.util.List;
import java.util.function.UnaryOperator;

public final class ReservationService {
    private final Database db;
    private final ReservationRepository reservations = new ReservationRepository();
    private final MessageRepository messages = new MessageRepository();

    public ReservationService(Database db) {
        this.db = db;
    }

    public Reservation create(NewReservation request) {
        Room room = RoomCatalog.require(request.roomNumber());
        if (request.guests() > room.capacity()) {
            throw new PousadaException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta até " + room.capacity() + " pessoas");
        }
        Quote quote = Quote.of(room, request.checkIn(), request.checkOut(), request.ratePlan());
        Reservation pending = Reservation.pending(room.number(), request.guestName(), request.guestEmail(),
                request.identity(), request.checkIn(), request.checkOut(), request.guests(),
                request.ratePlan(), quote);
        return db.tx(c -> {
            reservations.lockRoom(c, room.number());
            if (reservations.hasActiveOverlap(c, room.number(), request.checkIn(), request.checkOut())) {
                throw new PousadaException(ErrorCode.ROOM_UNAVAILABLE,
                        "O quarto " + room.number() + " já está reservado em alguma das noites");
            }
            return reservations.insert(c, pending);
        });
    }

    public Reservation get(long id) {
        return db.tx(c -> reservations.find(c, id).orElseThrow(() -> notFound(id)));
    }

    public Reservation confirm(long id) {
        return db.tx(c -> {
            Reservation confirmed = reservations.findForUpdate(c, id).orElseThrow(() -> notFound(id)).confirm();
            reservations.updateState(c, confirmed);
            messages.add(c, Channel.EMAIL, confirmed.guestEmail(), ConfirmationEmail.SUBJECT,
                    ConfirmationEmail.body(confirmed.guestName(), document(confirmed).text()));
            return confirmed;
        });
    }

    public Reservation cancel(long id) {
        return transition(id, Reservation::cancel);
    }

    public Reservation checkIn(long id) {
        return transition(id, Reservation::registerCheckIn);
    }

    public Reservation checkOut(long id) {
        return transition(id, Reservation::registerCheckOut);
    }

    /** Segunda via: só existe para reserva paga. */
    public Reservation requirePaid(long id) {
        Reservation reservation = get(id);
        if (!reservation.status().isPaid()) {
            throw new PousadaException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Reserva " + reservation.status() + " não tem comprovante");
        }
        return reservation;
    }

    public IssuedDocument document(Reservation reservation) {
        Room room = RoomCatalog.require(reservation.roomNumber());
        return reservation.identity().issue(reservation.stay(room));
    }

    public List<Message> messages() {
        return db.tx(messages::all);
    }

    private Reservation transition(long id, UnaryOperator<Reservation> change) {
        return db.tx(c -> {
            Reservation changed = change.apply(reservations.findForUpdate(c, id).orElseThrow(() -> notFound(id)));
            reservations.updateState(c, changed);
            return changed;
        });
    }

    private static PousadaException notFound(long id) {
        return new PousadaException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + id + " não existe");
    }
}

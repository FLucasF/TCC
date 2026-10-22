package pousada;

import pousada.domain.ConfirmationEmail;
import pousada.domain.Message;
import pousada.domain.NewReservation;
import pousada.domain.Reservation;
import pousada.domain.Room;
import pousada.domain.Rooms;
import pousada.persistence.Database;
import pousada.persistence.MessageRepository;
import pousada.persistence.ReservationRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class ReservationService {
    private final Database db;
    private final ReservationRepository reservations = new ReservationRepository();
    private final MessageRepository messages = new MessageRepository();

    public ReservationService(Database db) {
        this.db = db;
    }

    /** Confere quarto, capacidade e disponibilidade antes de gravar a reserva pendente. */
    public Reservation create(NewReservation request) {
        Room room = Rooms.find(request.roomNumber()).orElseThrow(() ->
                new BusinessException(ErrorCode.ROOM_NOT_FOUND, "Quarto inexistente: " + request.roomNumber()));
        if (request.guests() > room.capacity()) {
            throw new BusinessException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " pessoas");
        }
        return db.transaction(conn -> {
            reservations.lockRoom(conn, room.number());
            if (reservations.hasOverlap(conn, room.number(), request.checkIn(), request.checkOut())) {
                throw new BusinessException(ErrorCode.ROOM_UNAVAILABLE,
                        "O quarto " + room.number() + " já está reservado em alguma dessas noites");
            }
            return reservations.insert(conn, Reservation.pending(room, request));
        });
    }

    public Reservation get(long id) {
        return db.transaction(conn -> reservations.find(conn, id).orElseThrow(() -> notFound(id)));
    }

    /** Cancelar, check-in e check-out: bloqueia a reserva, aplica a mudança e grava. */
    public Reservation transition(long id, UnaryOperator<Reservation> change) {
        return transition(id, change, null);
    }

    /** Confirmar: a mudança e o e-mail ao hóspede são gravados juntos. */
    public Reservation confirm(long id) {
        return transition(id, Reservation::confirm, ConfirmationEmail::of);
    }

    private Reservation transition(long id, UnaryOperator<Reservation> change,
                                   Function<Reservation, Message> notification) {
        return db.transaction(conn -> {
            Reservation current = reservations.findForUpdate(conn, id).orElseThrow(() -> notFound(id));
            Reservation changed = change.apply(current);
            reservations.update(conn, changed);
            if (notification != null) {
                messages.insert(conn, notification.apply(changed));
            }
            return changed;
        });
    }

    public List<Message> messages() {
        return db.transaction(messages::all);
    }

    public List<Reservation> occupyingNight(LocalDate night) {
        return db.transaction(conn -> reservations.occupyingNight(conn, night));
    }

    public List<Reservation> startingBetween(LocalDate from, LocalDate to) {
        return db.transaction(conn -> reservations.startingBetween(conn, from, to));
    }

    private static BusinessException notFound(long id) {
        return new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva inexistente: " + id);
    }
}

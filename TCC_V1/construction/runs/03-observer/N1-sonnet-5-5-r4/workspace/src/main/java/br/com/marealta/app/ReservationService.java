package br.com.marealta.app;

import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomCatalog;
import br.com.marealta.notification.Notifier;
import br.com.marealta.notification.ReservationEvent;

import java.util.function.UnaryOperator;

public class ReservationService {
    private final RoomCatalog rooms;
    private final ReservationRepository reservations;
    private final Notifier notifier;

    public ReservationService(RoomCatalog rooms, ReservationRepository reservations, Notifier notifier) {
        this.rooms = rooms;
        this.reservations = reservations;
        this.notifier = notifier;
    }

    public synchronized Reservation create(CreateReservation cmd) {
        Room room = rooms.require(cmd.roomNumber());
        if (cmd.guests() > room.capacity()) {
            throw new DomainException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        if (reservations.existsActiveOverlap(room.number(), cmd.checkIn(), cmd.checkOut())) {
            throw new DomainException(ErrorCode.ROOM_UNAVAILABLE,
                    "O quarto " + room.number() + " já está reservado em alguma das noites pedidas");
        }
        return reservations.insert(Reservation.pending(room, cmd.guestName(), cmd.guestEmail(), cmd.guestCpf(),
                cmd.checkIn(), cmd.checkOut(), cmd.guests(), cmd.ratePlan()));
    }

    public Reservation get(long id) {
        return reservations.find(id).orElseThrow(() ->
                new DomainException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + id + " não existe"));
    }

    public synchronized Reservation confirm(long id) {
        Reservation confirmed = transition(id, Reservation::confirmed);
        notifier.publish(new ReservationEvent.Confirmed(confirmed));
        return confirmed;
    }

    public synchronized Reservation cancel(long id) {
        boolean wasConfirmed = get(id).status() == ReservationStatus.CONFIRMED;
        Reservation cancelled = transition(id, Reservation::cancelled);
        if (wasConfirmed) {
            notifier.publish(new ReservationEvent.Cancelled(cancelled));
        }
        return cancelled;
    }

    public synchronized Reservation checkIn(long id) {
        return transition(id, Reservation::checkedIn);
    }

    public synchronized Receipt checkOut(long id) {
        return receiptOf(transition(id, Reservation::checkedOut));
    }

    /** Segunda via: só existe para reserva paga. */
    public Receipt document(long id) {
        Reservation reservation = get(id);
        if (!reservation.wasPaid()) {
            throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Só existe comprovante para reserva paga; esta está " + reservation.status());
        }
        return receiptOf(reservation);
    }

    private Receipt receiptOf(Reservation reservation) {
        return Receipt.of(reservation, rooms.require(reservation.roomNumber()));
    }

    private Reservation transition(long id, UnaryOperator<Reservation> change) {
        Reservation changed = change.apply(get(id));
        reservations.update(changed);
        return changed;
    }
}

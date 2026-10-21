package pousada.service;

import java.util.function.UnaryOperator;
import pousada.domain.Quote;
import pousada.domain.Receipt;
import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;
import pousada.domain.Room;
import pousada.domain.Money;
import pousada.error.BusinessException;
import pousada.error.ErrorCode;

public class ReservationService {
    private final RoomCatalog rooms;
    private final ReservationRepository repository;
    private final Notifications notifications;

    public ReservationService(RoomCatalog rooms, ReservationRepository repository, Notifications notifications) {
        this.rooms = rooms;
        this.repository = repository;
        this.notifications = notifications;
    }

    public synchronized Reservation create(NewReservation req) {
        Room room = rooms.find(req.roomNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND, "Quarto inexistente: " + req.roomNumber()));
        if (req.guests() > room.capacity()) {
            throw new BusinessException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        boolean taken = repository.all().stream()
                .anyMatch(r -> r.room().equals(room)
                        && r.status() != ReservationStatus.CANCELLED
                        && r.overlaps(req.checkIn(), req.checkOut()));
        if (taken) {
            throw new BusinessException(ErrorCode.ROOM_UNAVAILABLE,
                    "O quarto " + room.number() + " já está reservado em alguma das noites");
        }
        Quote quote = Quote.of(room, req.checkIn(), req.checkOut(), req.ratePlan());
        Reservation reservation = new Reservation(repository.nextId(), room, req.guestName(), req.guestEmail(),
                req.guestCpf(), req.checkIn(), req.checkOut(), req.guests(), req.ratePlan(), quote,
                ReservationStatus.PENDING, Money.ZERO, null);
        repository.save(reservation);
        return reservation;
    }

    public Reservation get(int id) {
        return repository.find(id).orElseThrow(() ->
                new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva inexistente: " + id));
    }

    public synchronized Reservation confirm(int id) {
        Reservation confirmed = change(id, Reservation::confirm);
        notifications.reservationConfirmed(confirmed);
        return confirmed;
    }

    public synchronized Reservation cancel(int id) {
        return change(id, Reservation::cancel);
    }

    public synchronized Reservation checkIn(int id) {
        return change(id, Reservation::doCheckIn);
    }

    public synchronized Receipt checkOut(int id) {
        return Receipt.of(change(id, Reservation::doCheckOut));
    }

    public Receipt receipt(int id) {
        Reservation reservation = get(id);
        reservation.requirePaid();
        return Receipt.of(reservation);
    }

    private Reservation change(int id, UnaryOperator<Reservation> transition) {
        Reservation changed = transition.apply(get(id));
        repository.save(changed);
        return changed;
    }
}

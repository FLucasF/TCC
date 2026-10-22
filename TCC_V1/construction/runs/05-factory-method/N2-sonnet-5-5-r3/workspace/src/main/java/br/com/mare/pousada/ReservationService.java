package br.com.mare.pousada;

import java.util.List;

public class ReservationService {
    private final ReservationRepository reservations;
    private final Notifier notifier;

    public ReservationService(ReservationRepository reservations, Notifier notifier) {
        this.reservations = reservations;
        this.notifier = notifier;
    }

    public synchronized Reservation create(BookingRequest request) {
        Room room = RoomCatalog.find(request.roomNumber())
                .orElseThrow(() -> new PousadaException(ErrorCode.ROOM_NOT_FOUND,
                        "Quarto " + request.roomNumber() + " não existe"));
        if (request.guests() > room.capacity()) {
            throw new PousadaException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        boolean taken = reservations.findAll().stream()
                .filter(r -> r.room().number() == room.number())
                .filter(r -> r.status() != ReservationStatus.CANCELLED)
                .anyMatch(r -> r.overlaps(request.checkIn(), request.checkOut()));
        if (taken) {
            throw new PousadaException(ErrorCode.ROOM_UNAVAILABLE,
                    "O quarto " + room.number() + " já está reservado em alguma das noites pedidas");
        }
        return reservations.insert(Reservation.pending(room, request.guestName(), request.guestEmail(),
                request.identity(), request.checkIn(), request.checkOut(), request.guests(), request.ratePlan()));
    }

    public Reservation get(int id) {
        return reservations.find(id).orElseThrow(() -> new PousadaException(ErrorCode.RESERVATION_NOT_FOUND,
                "Reserva " + id + " não existe"));
    }

    public synchronized Reservation confirm(int id) {
        Reservation reservation = get(id);
        reservation.confirm();
        reservations.update(reservation);
        notifier.reservationConfirmed(reservation);
        return reservation;
    }

    public synchronized Reservation cancel(int id) {
        Reservation reservation = get(id);
        reservation.cancel();
        reservations.update(reservation);
        return reservation;
    }

    public synchronized Reservation checkIn(int id) {
        Reservation reservation = get(id);
        reservation.startStay();
        reservations.update(reservation);
        return reservation;
    }

    public synchronized Document checkOut(int id) {
        Reservation reservation = get(id);
        reservation.endStay();
        reservations.update(reservation);
        return reservation.identity().document(reservation);
    }

    /** Segunda via: só existe para reserva paga. */
    public Document document(int id) {
        Reservation reservation = get(id);
        if (!reservation.status().isPaid()) {
            throw new PousadaException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "A reserva ainda não foi paga; não há documento");
        }
        return reservation.identity().document(reservation);
    }

    public List<Reservation> all() {
        return reservations.findAll();
    }
}

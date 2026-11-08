package br.com.marealta.pousada.service;

import br.com.marealta.pousada.domain.ErrorCode;
import br.com.marealta.pousada.domain.PousadaException;
import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.domain.Receipt;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationStatus;
import br.com.marealta.pousada.domain.Room;
import br.com.marealta.pousada.notification.ReservationListener;
import br.com.marealta.pousada.persistence.ReservationRepository;
import br.com.marealta.pousada.persistence.RoomRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

public class ReservationService {
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final List<ReservationListener> listeners;

    public ReservationService(RoomRepository rooms, ReservationRepository reservations,
                              List<ReservationListener> listeners) {
        this.rooms = rooms;
        this.reservations = reservations;
        this.listeners = List.copyOf(listeners);
    }

    public Reservation create(int roomNumber, String guestName, String guestEmail, String guestCpf,
                              LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        if (guests < 1 || !checkOut.isAfter(checkIn)) {
            throw new PousadaException(ErrorCode.INVALID_REQUEST, "Datas ou número de hóspedes inválidos");
        }
        Room room = rooms.findByNumber(roomNumber)
                .orElseThrow(() -> new PousadaException(ErrorCode.ROOM_NOT_FOUND, "Quarto não encontrado: " + roomNumber));
        Reservation reservation = Reservation.create(room, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan);
        reservations.insertIfAvailable(reservation);
        return reservation;
    }

    public Reservation confirm(long id) {
        Reservation r = get(id);
        ReservationStatus previous = r.status();
        r.confirm();
        save(r, previous);
        listeners.forEach(l -> l.onConfirmed(r));
        return r;
    }

    public Reservation cancel(long id) {
        Reservation r = get(id);
        ReservationStatus previous = r.status();
        r.cancel();
        save(r, previous);
        listeners.forEach(l -> l.onCancelled(r, previous));
        return r;
    }

    public Reservation checkIn(long id) {
        return transition(id, Reservation::startStay);
    }

    public Receipt checkOut(long id) {
        return transition(id, Reservation::endStay).receipt();
    }

    public Receipt document(long id) {
        return get(id).receipt();
    }

    public Reservation get(long id) {
        return reservations.findById(id)
                .orElseThrow(() -> new PousadaException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva não encontrada: " + id));
    }

    private Reservation transition(long id, Consumer<Reservation> change) {
        Reservation r = get(id);
        ReservationStatus previous = r.status();
        change.accept(r);
        save(r, previous);
        return r;
    }

    private void save(Reservation r, ReservationStatus previous) {
        if (!reservations.update(r, previous)) {
            throw new PousadaException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "A situação da reserva mudou enquanto a operação era feita");
        }
    }
}

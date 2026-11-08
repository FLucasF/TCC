package br.com.mare.domain;

import br.com.mare.persistence.JdbcMessageRepository;
import br.com.mare.persistence.JdbcReservationRepository;

import java.time.LocalDate;
import java.util.function.Consumer;

public class ReservationService {
    private final JdbcReservationRepository reservations;
    private final JdbcMessageRepository messages;

    public ReservationService(JdbcReservationRepository reservations, JdbcMessageRepository messages) {
        this.reservations = reservations;
        this.messages = messages;
    }

    /** Dados já validados em formato (campos presentes, datas válidas, saída depois da entrada, ao menos 1 hóspede). */
    public synchronized Reservation create(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                           LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Room room = Hotel.requireRoom(roomNumber);
        if (guests > room.capacity()) {
            throw new BusinessException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + roomNumber + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        boolean taken = reservations.findActiveByRoom(roomNumber).stream()
                .anyMatch(r -> r.overlaps(checkIn, checkOut));
        if (taken) {
            throw new BusinessException(ErrorCode.ROOM_UNAVAILABLE,
                    "O quarto " + roomNumber + " já está reservado em alguma das noites");
        }
        Reservation reservation = Reservation.book(room, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan);
        reservations.insert(reservation);
        return reservation;
    }

    public Reservation find(String id) {
        long parsed;
        try {
            parsed = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw notFound(id);
        }
        return reservations.findById(parsed).orElseThrow(() -> notFound(id));
    }

    public synchronized Reservation confirm(String id) {
        Reservation reservation = change(id, Reservation::confirm);
        messages.add(Message.reservationConfirmed(reservation, receiptOf(reservation)));
        return reservation;
    }

    public synchronized Reservation cancel(String id) {
        return change(id, Reservation::cancel);
    }

    public synchronized Reservation checkIn(String id) {
        return change(id, Reservation::registerCheckIn);
    }

    public synchronized Receipt checkOut(String id) {
        return receiptOf(change(id, Reservation::registerCheckOut));
    }

    /** Segunda via: só existe para reserva paga. */
    public Receipt document(String id) {
        Reservation reservation = find(id);
        if (!reservation.isPaid()) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Reserva " + reservation.status() + " não tem comprovante");
        }
        return receiptOf(reservation);
    }

    private Reservation change(String id, Consumer<Reservation> operation) {
        Reservation reservation = find(id);
        operation.accept(reservation);
        reservations.update(reservation);
        return reservation;
    }

    private Receipt receiptOf(Reservation reservation) {
        return Receipt.of(reservation, Hotel.requireRoom(reservation.roomNumber()));
    }

    private BusinessException notFound(String id) {
        return new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + id + " não existe");
    }
}

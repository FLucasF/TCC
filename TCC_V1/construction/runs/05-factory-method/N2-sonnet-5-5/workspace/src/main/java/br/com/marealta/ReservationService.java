package br.com.marealta;

import java.time.LocalDate;
import java.util.function.Consumer;

public class ReservationService {
    private final RoomCatalog rooms;
    private final PriceCalculator prices;
    private final ReservationRepository reservations;
    private final MessageRepository messages;

    public ReservationService(RoomCatalog rooms, PriceCalculator prices, ReservationRepository reservations,
                              MessageRepository messages) {
        this.rooms = rooms;
        this.prices = prices;
        this.reservations = reservations;
        this.messages = messages;
    }

    public synchronized Reservation create(int roomNumber, String guestName, String guestEmail,
                                           GuestIdentity identity, LocalDate checkIn, LocalDate checkOut,
                                           int guests, RatePlan plan) {
        if (guests < 1) throw AppException.invalid("É preciso pelo menos um hóspede.");
        if (!checkOut.isAfter(checkIn)) throw AppException.invalid("A saída precisa ser depois da entrada.");
        Room room = rooms.find(roomNumber)
                .orElseThrow(() -> new AppException(404, "ROOM_NOT_FOUND", "Quarto " + roomNumber + " não existe."));
        if (guests > room.type().capacity()) {
            throw new AppException(422, "CAPACITY_EXCEEDED",
                    "O quarto " + roomNumber + " comporta até " + room.type().capacity() + " pessoas.");
        }
        boolean taken = reservations.findAll().stream()
                .filter(r -> r.status() != ReservationStatus.CANCELLED)
                .filter(r -> r.room().number() == roomNumber)
                .anyMatch(r -> r.overlaps(checkIn, checkOut));
        if (taken) {
            throw new AppException(409, "ROOM_UNAVAILABLE", "O quarto " + roomNumber + " já está reservado nesse período.");
        }
        Reservation reservation = Reservation.pending(room, guestName, guestEmail, identity, checkIn, checkOut,
                guests, plan, prices.calculate(room.type(), checkIn, checkOut, plan));
        reservations.insert(reservation);
        return reservation;
    }

    public Reservation get(long id) {
        return reservations.find(id).orElseThrow(() ->
                new AppException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe."));
    }

    public synchronized Reservation confirm(long id) {
        Reservation r = change(id, Reservation::confirm);
        messages.save(ConfirmationEmail.of(r));
        return r;
    }

    public synchronized Reservation cancel(long id) {
        return change(id, Reservation::cancel);
    }

    public synchronized Reservation checkIn(long id) {
        return change(id, Reservation::startStay);
    }

    public synchronized Reservation checkOut(long id) {
        return change(id, Reservation::endStay);
    }

    /** Segunda via: só existe para reserva paga. */
    public Reservation paidReservation(long id) {
        Reservation r = get(id);
        if (!r.status().paid()) {
            throw AppException.notAllowed("Só existe documento para reserva paga; situação atual: " + r.status() + ".");
        }
        return r;
    }

    private Reservation change(long id, Consumer<Reservation> operation) {
        Reservation r = get(id);
        operation.accept(r);
        reservations.update(r);
        return r;
    }
}

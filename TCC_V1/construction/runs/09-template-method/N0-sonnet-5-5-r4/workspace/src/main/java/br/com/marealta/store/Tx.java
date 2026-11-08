package br.com.marealta.store;

import br.com.marealta.domain.Message;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface Tx {
    List<Room> rooms();

    Optional<Room> room(int number);

    /** Serializes concurrent bookings of the same room until the transaction ends. */
    void lockRoom(int number);

    /** Whether a non-cancelled reservation of the room overlaps [checkIn, checkOut). */
    boolean roomBooked(int roomNumber, LocalDate checkIn, LocalDate checkOut);

    /** Saves a new reservation and returns it with its assigned id. */
    Reservation insert(Reservation reservation);

    void update(Reservation reservation);

    Optional<Reservation> reservation(long id);

    /** Same as {@link #reservation(long)}, locking the reservation until the transaction ends. */
    Optional<Reservation> reservationForUpdate(long id);

    /** Reservations whose check-in is in [from, toExclusive). */
    List<Reservation> reservationsCheckingInBetween(LocalDate from, LocalDate toExclusive);

    /** Every reservation, whatever its status, whose stay [checkIn, checkOut) covers the night. */
    List<Reservation> reservationsCoveringNight(LocalDate night);

    Message saveMessage(Message message);

    List<Message> messages();
}

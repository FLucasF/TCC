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

    /** Serializa as reservas concorrentes do mesmo quarto até o fim da transação. */
    void lockRoom(int number);

    /** Há reserva não cancelada do quarto que ocupe alguma noite de [checkIn, checkOut)? */
    boolean hasActiveOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut);

    Reservation insert(Reservation draft);

    /** Lê a reserva travando-a até o fim da transação. */
    Optional<Reservation> reservationForUpdate(long id);

    Optional<Reservation> reservation(long id);

    void update(Reservation reservation);

    /** Reservas com entrada em [from, to], por data de entrada e depois número. */
    List<Reservation> startingBetween(LocalDate from, LocalDate to);

    /** Reservas com checkIn <= night < checkOut (qualquer situação), por quarto. */
    List<Reservation> occupying(LocalDate night);

    Message addMessage(String channel, String to, String subject, String body);

    List<Message> messages();
}

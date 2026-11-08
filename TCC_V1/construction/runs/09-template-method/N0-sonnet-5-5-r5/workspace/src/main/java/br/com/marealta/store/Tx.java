package br.com.marealta.store;

import br.com.marealta.domain.Message;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface Tx {
    List<Room> rooms();

    /** Com {@code lock}, serializa quem mexe no mesmo quarto até o fim da transação. */
    Optional<Room> room(int number, boolean lock);

    /** Existe reserva não cancelada do quarto que cruze alguma noite de [checkIn, checkOut)? */
    boolean overlaps(int roomNumber, LocalDate checkIn, LocalDate checkOut);

    Reservation insert(Reservation reservation);

    Optional<Reservation> reservation(long id, boolean lock);

    void update(Reservation reservation);

    Message addMessage(String channel, String to, String subject, String body);

    List<Message> messages();

    /** Reservas com entrada em [from, toExclusive), ordenadas por entrada e id. */
    List<Reservation> startingBetween(LocalDate from, LocalDate toExclusive);

    /** Reservas que ocupam a noite indicada (entrada <= noite < saída), por quarto. */
    List<Reservation> stayingOn(LocalDate night);
}

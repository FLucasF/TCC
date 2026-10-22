package br.com.marealta.store;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.event.Message;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Persistência. Cada chamada a {@link #tx} é atômica: ou tudo é gravado, ou nada. */
public interface Store {
    <T> T tx(Function<Tx, T> work);

    interface Tx {
        List<Room> rooms();

        Optional<Room> room(int number);

        /** Trava o quarto até o fim da transação, serializando reservas concorrentes dele. */
        void lockRoom(int number);

        boolean hasOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut);

        /** Grava uma nova reserva (o id do argumento é ignorado) e a devolve com o id gerado. */
        Reservation insert(Reservation r);

        Optional<Reservation> find(long id);

        /** Como {@link #find}, mas trava a reserva até o fim da transação. */
        Optional<Reservation> findForUpdate(long id);

        void update(Reservation r);

        List<Reservation> all();

        void addMessage(Message m);

        List<Message> messages();
    }
}

package br.com.maretalta.store;

import br.com.maretalta.domain.Message;
import br.com.maretalta.domain.Reservation;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Persistência. Tudo acontece dentro de uma transação. */
public interface Store {

    <T> T inTransaction(Function<Tx, T> work);

    interface Tx {
        /** Serializa quem reserva o mesmo quarto até o fim da transação. */
        void lockRoom(int roomNumber);

        /** Há reserva não cancelada do quarto que cruze alguma noite de [checkIn, checkOut)? */
        boolean hasOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut);

        /** Grava uma reserva nova e devolve-a com o id atribuído (a partir de 1). */
        Reservation insert(Reservation draft);

        /** Busca a reserva travando-a até o fim da transação. */
        Optional<Reservation> findForUpdate(long id);

        Optional<Reservation> find(long id);

        void update(Reservation r);

        /** Todas as reservas (qualquer situação) com entrada no mês, por entrada e id. */
        List<Reservation> startingIn(YearMonth month);

        /** Reservas pagas (confirmada/check-in/check-out) que ocupam o quarto na noite, por quarto. */
        List<Reservation> paidOccupying(LocalDate night);

        Message addMessage(String channel, String to, String subject, String body);

        List<Message> messages();
    }
}

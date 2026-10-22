package br.com.mealta.store;

import br.com.mealta.domain.Reservation;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    /** Insere (id nulo, recebe o próximo número) ou atualiza a reserva. */
    Reservation save(Reservation reservation);

    Optional<Reservation> findById(long id);

    /** Como findById, mas segura a reserva até o fim da transação. */
    Optional<Reservation> lockById(long id);

    /** Serializa, até o fim da transação, quem está reservando o mesmo quarto. */
    void lockRoom(int roomNumber);

    /** Há reserva não cancelada do quarto em alguma noite de [checkIn, checkOut)? */
    boolean hasActiveOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut);

    /** Reservas com entrada no mês, por data de entrada e número. */
    List<Reservation> startingIn(YearMonth month);

    /** Reservas pagas que ocupam um quarto na noite. */
    List<Reservation> paidOccupying(LocalDate night);
}

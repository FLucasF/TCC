package br.com.marealta.service;

import br.com.marealta.domain.Reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    /** Grava uma reserva nova e devolve-a com o id gerado. */
    Reservation insert(Reservation reservation);

    void update(Reservation reservation);

    Optional<Reservation> find(long id);

    /** Busca travando a reserva até o fim da transação. */
    Optional<Reservation> findForUpdate(long id);

    /** Existe reserva não cancelada do quarto que ocupe alguma noite de [checkIn, checkOut)? */
    boolean existsOverlapping(int roomNumber, LocalDate checkIn, LocalDate checkOut);

    /** Reservas pagas que ocupam o quarto na noite indicada, por número do quarto. */
    List<Reservation> findPaidOccupying(LocalDate night);

    /** Reservas com entrada em [from, to), por data de entrada e depois id. */
    List<Reservation> findCheckingInBetween(LocalDate from, LocalDate to);
}

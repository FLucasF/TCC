package br.com.marealta.app;

import br.com.marealta.domain.Reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    /** Grava uma reserva nova e devolve-a já com o número. */
    Reservation insert(Reservation reservation);

    void update(Reservation reservation);

    Optional<Reservation> find(long id);

    List<Reservation> findAll();

    /** Existe reserva não cancelada que ocupa o quarto em alguma noite de [checkIn, checkOut)? */
    boolean existsActiveOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut);
}

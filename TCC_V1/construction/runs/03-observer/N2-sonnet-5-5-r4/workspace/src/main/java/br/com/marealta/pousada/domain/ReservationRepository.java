package br.com.marealta.pousada.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {

    /** Grava uma reserva nova e devolve-a com o número atribuído. */
    Reservation insert(Reservation draft);

    void update(Reservation reservation);

    Optional<Reservation> find(long id);

    List<Reservation> findAll();

    /** Há reserva não cancelada do quarto que ocupe alguma noite de [checkIn, checkOut)? */
    boolean existsOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut);
}

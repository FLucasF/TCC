package pousada.repo;

import pousada.domain.Reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    /** Grava a reserva nova, dá a ela o próximo número (a partir de 1) e a devolve. */
    Reservation insert(Reservation reservation);

    void update(Reservation reservation);

    Optional<Reservation> find(long id);

    /** Há reserva não cancelada no quarto em alguma noite de [checkIn, checkOut)? */
    boolean hasOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut);

    /** Reservas com entrada em [from, to], em ordem de entrada e depois de número. */
    List<Reservation> findCheckInBetween(LocalDate from, LocalDate to);

    /** Reservas pagas que ocupam um quarto na noite, em ordem de número do quarto. */
    List<Reservation> findPaidOccupying(LocalDate night);
}

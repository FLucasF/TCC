package br.com.marealta;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    /** Grava a reserva nova e atribui o número dela. */
    void insert(Reservation reservation);

    void update(Reservation reservation);

    Optional<Reservation> find(long id);

    List<Reservation> findAll();
}

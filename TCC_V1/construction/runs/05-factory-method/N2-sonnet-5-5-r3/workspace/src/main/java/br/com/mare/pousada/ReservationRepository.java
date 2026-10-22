package br.com.mare.pousada;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    /** Grava a reserva nova e devolve a mesma reserva já com o número. */
    Reservation insert(Reservation reservation);

    void update(Reservation reservation);

    Optional<Reservation> find(int id);

    List<Reservation> findAll();
}

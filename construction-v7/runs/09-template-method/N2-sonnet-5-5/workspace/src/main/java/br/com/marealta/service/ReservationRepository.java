package br.com.marealta.service;

import br.com.marealta.domain.Reservation;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    int nextId();

    void save(Reservation reservation);

    Optional<Reservation> find(int id);

    List<Reservation> all();
}

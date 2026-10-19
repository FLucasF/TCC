package br.com.marealta.service;

import br.com.marealta.domain.Reservation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryReservationRepository implements ReservationRepository {
    private final Map<Integer, Reservation> byId = new LinkedHashMap<>();

    @Override
    public int nextId() { return byId.size() + 1; }

    @Override
    public void save(Reservation reservation) { byId.put(reservation.id(), reservation); }

    @Override
    public Optional<Reservation> find(int id) { return Optional.ofNullable(byId.get(id)); }

    @Override
    public List<Reservation> all() { return new ArrayList<>(byId.values()); }
}

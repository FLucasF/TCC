package br.com.pousadamarealta.reservas.events;

import br.com.pousadamarealta.reservas.domain.Reservation;
import br.com.pousadamarealta.reservas.domain.Room;

public record ReservationConfirmed(Reservation reservation, Room room) implements DomainEvent {}

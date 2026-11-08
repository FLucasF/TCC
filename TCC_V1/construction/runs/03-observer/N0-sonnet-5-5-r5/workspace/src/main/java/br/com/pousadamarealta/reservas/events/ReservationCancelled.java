package br.com.pousadamarealta.reservas.events;

import br.com.pousadamarealta.reservas.domain.Reservation;
import br.com.pousadamarealta.reservas.domain.Room;

/** Publicado só quando a reserva cancelada já estava confirmada; o reembolso está em reservation.refund(). */
public record ReservationCancelled(Reservation reservation, Room room) implements DomainEvent {}

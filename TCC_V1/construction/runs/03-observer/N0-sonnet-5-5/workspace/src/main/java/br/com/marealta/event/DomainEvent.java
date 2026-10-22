package br.com.marealta.event;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;

import java.math.BigDecimal;

/** Fatos relevantes do negócio, dos quais quem quiser ser avisado se inscreve. */
public sealed interface DomainEvent {
    Reservation reservation();
    Room room();

    /** Reserva paga e confirmada. */
    record ReservationConfirmed(Reservation reservation, Room room) implements DomainEvent {}

    /**
     * Reserva cancelada. {@code wasConfirmed} diz se já estava paga; {@code refund} é o valor devolvido
     * (zero quando não há nada a devolver).
     */
    record ReservationCancelled(Reservation reservation, Room room, boolean wasConfirmed, BigDecimal refund)
            implements DomainEvent {}
}

package br.com.marealta.reservas.notification;

import br.com.marealta.reservas.domain.Reservation;
import br.com.marealta.reservas.domain.Room;

/** A reserva já vem cancelada; {@code reservation.wasPaid()} diz se havia pagamento e {@code refund()} o que volta. */
public record ReservationCancelled(Reservation reservation, Room room) {
}

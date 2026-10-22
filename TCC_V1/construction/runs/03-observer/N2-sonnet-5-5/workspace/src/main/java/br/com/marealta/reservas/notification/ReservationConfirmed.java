package br.com.marealta.reservas.notification;

import br.com.marealta.reservas.domain.Reservation;
import br.com.marealta.reservas.domain.Room;

public record ReservationConfirmed(Reservation reservation, Room room) {
}

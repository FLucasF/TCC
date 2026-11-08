package com.marealta.events;

import com.marealta.domain.Reservation;
import com.marealta.domain.ReservationStatus;
import com.marealta.domain.Room;

/** Algo que aconteceu com uma reserva. {@code previousStatus} é a situação de antes do acontecimento. */
public record ReservationEvent(Type type, Reservation reservation, Room room, ReservationStatus previousStatus) {
    public enum Type { CONFIRMED, CANCELLED, CHECKED_IN, CHECKED_OUT }
}

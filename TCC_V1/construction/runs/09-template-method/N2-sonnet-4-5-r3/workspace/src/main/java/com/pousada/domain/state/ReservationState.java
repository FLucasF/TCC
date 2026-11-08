package com.pousada.domain.state;

import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;

public interface ReservationState {
    ReservationStatus getStatus();
    void confirm(Reservation reservation);
    void cancel(Reservation reservation);
    void checkIn(Reservation reservation);
    void checkOut(Reservation reservation);
}

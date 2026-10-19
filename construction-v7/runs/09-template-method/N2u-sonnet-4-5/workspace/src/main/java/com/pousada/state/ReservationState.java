package com.pousada.state;

import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;

import java.math.BigDecimal;

public interface ReservationState {
    ReservationStatus getStatus();
    void confirm(Reservation reservation, BigDecimal amountPaid);
    void cancel(Reservation reservation);
    void checkIn(Reservation reservation);
    void checkOut(Reservation reservation);
}

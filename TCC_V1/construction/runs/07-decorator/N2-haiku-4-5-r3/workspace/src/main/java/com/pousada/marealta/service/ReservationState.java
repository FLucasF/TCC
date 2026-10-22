package com.pousada.marealta.service;

import com.pousada.marealta.domain.Reservation;

public interface ReservationState {
    Reservation confirm(Reservation reservation);
    Reservation checkIn(Reservation reservation);
    Reservation checkOut(Reservation reservation);
    Reservation cancel(Reservation reservation);
}

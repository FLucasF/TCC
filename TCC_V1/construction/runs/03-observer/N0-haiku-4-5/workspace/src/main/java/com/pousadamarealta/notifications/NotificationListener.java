package com.pousadamarealta.notifications;

import com.pousadamarealta.models.Reservation;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface NotificationListener {
    void onReservationConfirmed(Reservation reservation);
    void onReservationCancelled(Reservation reservation, BigDecimal refundAmount);
}

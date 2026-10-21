package com.pousada.marealta.notification;

import com.pousada.marealta.model.Reservation;

public interface NotificationHandler {
    void handleConfirmation(Reservation reservation);
    void handleCancellation(Reservation reservation);
}

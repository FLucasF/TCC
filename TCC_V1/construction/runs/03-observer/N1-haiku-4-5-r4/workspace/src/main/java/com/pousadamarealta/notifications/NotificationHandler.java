package com.pousadamarealta.notifications;

import com.pousadamarealta.models.Reservation;
import com.pousadamarealta.repository.MessageRepository;

public interface NotificationHandler {
    void handleConfirmation(Reservation reservation, MessageRepository messageRepo) throws Exception;
    void handleCancellation(Reservation reservation, MessageRepository messageRepo) throws Exception;
}

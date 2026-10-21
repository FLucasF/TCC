package com.pousada.marealta.notification;

import java.util.ArrayList;
import java.util.List;
import com.pousada.marealta.model.Reservation;

public class NotificationService {
    private final List<NotificationHandler> handlers = new ArrayList<>();

    public void registerHandler(NotificationHandler handler) {
        handlers.add(handler);
    }

    public void notifyConfirmation(Reservation reservation) {
        for (NotificationHandler handler : handlers) {
            handler.handleConfirmation(reservation);
        }
    }

    public void notifyCancellation(Reservation reservation) {
        for (NotificationHandler handler : handlers) {
            handler.handleCancellation(reservation);
        }
    }
}

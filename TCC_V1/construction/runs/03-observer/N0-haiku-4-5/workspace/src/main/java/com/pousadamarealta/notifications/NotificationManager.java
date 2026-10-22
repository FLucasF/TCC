package com.pousadamarealta.notifications;

import com.pousadamarealta.models.Reservation;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class NotificationManager {
    private static final List<NotificationListener> listeners = new ArrayList<>();

    static {
        register(new GovernanceNotificationListener());
        register(new FinanceNotificationListener());
    }

    public static void register(NotificationListener listener) {
        listeners.add(listener);
    }

    public static void notifyReservationConfirmed(Reservation reservation) {
        for (NotificationListener listener : listeners) {
            listener.onReservationConfirmed(reservation);
        }
    }

    public static void notifyReservationCancelled(Reservation reservation, BigDecimal refundAmount) {
        for (NotificationListener listener : listeners) {
            listener.onReservationCancelled(reservation, refundAmount);
        }
    }
}

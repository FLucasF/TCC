package com.pousadamarealta.notification;

import com.pousadamarealta.model.Reservation;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {
    private final List<ReservationNotifier> notifiers = new ArrayList<>();

    public void register(ReservationNotifier notifier) {
        notifiers.add(notifier);
    }

    public void notifyConfirmed(Reservation reservation) throws Exception {
        for (ReservationNotifier notifier : notifiers) {
            notifier.onReservationConfirmed(reservation);
        }
    }

    public void notifyCancelled(Reservation reservation) throws Exception {
        for (ReservationNotifier notifier : notifiers) {
            notifier.onReservationCancelled(reservation);
        }
    }
}

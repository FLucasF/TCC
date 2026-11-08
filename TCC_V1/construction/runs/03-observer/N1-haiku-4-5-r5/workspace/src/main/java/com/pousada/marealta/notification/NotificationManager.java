package com.pousada.marealta.notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationManager {
    private static final List<ReservationNotifier> notifiers = new ArrayList<>();

    public static void registerNotifier(ReservationNotifier notifier) {
        notifiers.add(notifier);
    }

    public static void notify(ReservationEvent event) throws Exception {
        for (ReservationNotifier notifier : notifiers) {
            notifier.handleEvent(event);
        }
    }
}

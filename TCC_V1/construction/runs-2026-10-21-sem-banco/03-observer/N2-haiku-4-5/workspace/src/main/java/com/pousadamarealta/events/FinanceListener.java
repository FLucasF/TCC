package com.pousadamarealta.events;

import com.pousadamarealta.domain.ReservationStatus;

public class FinanceListener implements ReservationEventListener {
    private final NotificationService notificationService;

    public FinanceListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void onReservationEvent(ReservationEvent event) {
        if (event.getType() == ReservationEvent.Type.CONFIRMED) {
            notificationService.notifyFinanceOnConfirmation(event.getReservation());
        } else if (event.getType() == ReservationEvent.Type.CANCELLED) {
            ReservationStatus previousStatus = event.getPreviousStatus();
            if (previousStatus == ReservationStatus.CONFIRMED ||
                previousStatus == ReservationStatus.CHECKED_IN) {
                notificationService.notifyFinanceOnCancellation(event.getReservation());
            }
        }
    }
}

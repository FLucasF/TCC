package com.pousadamarealta.events;

import com.pousadamarealta.domain.ReservationStatus;

public class GovernanceListener implements ReservationEventListener {
    private final NotificationService notificationService;

    public GovernanceListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void onReservationEvent(ReservationEvent event) {
        if (event.getType() == ReservationEvent.Type.CONFIRMED) {
            notificationService.notifyGovernanceOnConfirmation(event.getReservation());
        } else if (event.getType() == ReservationEvent.Type.CANCELLED) {
            ReservationStatus previousStatus = event.getPreviousStatus();
            if (previousStatus == ReservationStatus.CONFIRMED ||
                previousStatus == ReservationStatus.CHECKED_IN) {
                notificationService.notifyGovernanceOnCancellation(event.getReservation());
            }
        }
    }
}

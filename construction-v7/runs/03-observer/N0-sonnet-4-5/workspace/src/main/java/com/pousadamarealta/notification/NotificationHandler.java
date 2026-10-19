package com.pousadamarealta.notification;

import java.util.List;

public interface NotificationHandler {
    List<NotificationMessage> handle(ReservationEvent event);
}

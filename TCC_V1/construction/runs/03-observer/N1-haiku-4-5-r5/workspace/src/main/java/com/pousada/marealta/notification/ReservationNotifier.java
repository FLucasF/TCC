package com.pousada.marealta.notification;

public interface ReservationNotifier {
    void handleEvent(ReservationEvent event) throws Exception;
}

package br.marealta.service;

import br.marealta.model.Reservation;
import java.util.List;

public interface NotificationHandler {
    void onReservationConfirmed(Reservation reservation);
    void onReservationCancelled(Reservation reservation);
}

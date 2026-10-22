package hotel.notifications;

import hotel.reservations.Reservation;

/** Listens to reservation events and reacts accordingly. */
public interface ReservationEventListener {

    void onReservationConfirmed(Reservation reservation);

    void onReservationCancelled(Reservation reservation, boolean wasPreviouslyConfirmed);
}

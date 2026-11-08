package br.com.pousadamarealta.domain.events;

public interface ReservationEventListener {
    void onReservationConfirmed(ReservationConfirmed event);
    void onReservationCancelled(ReservationCancelled event);
}

package br.com.marealta.pousada.events;

/** Anyone who wants to be told about reservation events implements this and is registered on the bus. */
public interface ReservationListener {
    void on(ReservationEvent event);
}

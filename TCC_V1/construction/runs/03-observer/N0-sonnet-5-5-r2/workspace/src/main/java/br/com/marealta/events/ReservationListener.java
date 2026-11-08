package br.com.marealta.events;

/**
 * Quem quer ser avisado implementa esta interface e é registrado em {@link ReservationEvents}
 * (ver Main). Para um novo tipo de aviso, basta um novo método com implementação vazia.
 */
public interface ReservationListener {
    default void onConfirmed(ReservationEvent.Confirmed event) {
    }

    default void onCancelled(ReservationEvent.Cancelled event) {
    }
}

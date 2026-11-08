package com.marealta.events;

/** Quem quer ser avisado do que acontece com as reservas. Cada ouvinte decide sozinho se o evento lhe interessa. */
public interface ReservationListener {
    void onEvent(ReservationEvent event);
}

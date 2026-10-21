package br.com.marealta.notification;

/** Quem quer ser avisado do que acontece com as reservas; ignora os eventos que não lhe interessam. */
public interface ReservationListener {

    void on(ReservationEvent event);
}

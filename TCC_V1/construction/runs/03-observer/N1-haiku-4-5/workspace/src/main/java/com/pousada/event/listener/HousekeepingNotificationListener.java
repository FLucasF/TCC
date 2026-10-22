package com.pousada.event.listener;

import com.pousada.database.MessageRepository;
import com.pousada.event.ReservationCancelledEvent;
import com.pousada.event.ReservationConfirmedEvent;
import com.pousada.event.ReservationEventListener;
import com.pousada.model.Reservation;
import com.pousada.service.FormattingService;

public class HousekeepingNotificationListener implements ReservationEventListener {
  private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";

  @Override
  public void onReservationConfirmed(ReservationConfirmedEvent event) {
    Reservation reservation = event.getReservation();
    String subject = "Reserva confirmada - quarto " + reservation.roomNumber;
    String body =
        "Entrada: "
            + FormattingService.formatDate(reservation.checkIn)
            + "\n"
            + "Saída: "
            + FormattingService.formatDate(reservation.checkOut)
            + "\n"
            + "Hóspedes: "
            + reservation.guests;

    MessageRepository.save("EMAIL", HOUSEKEEPING_EMAIL, subject, body);
  }

  @Override
  public void onReservationCancelled(ReservationCancelledEvent event) {
    if (!event.wasConfirmed()) {
      return;
    }

    Reservation reservation = event.getReservation();
    String subject = "Reserva cancelada - quarto " + reservation.roomNumber;
    String body =
        "Entrada: "
            + FormattingService.formatDate(reservation.checkIn)
            + "\n"
            + "Saída: "
            + FormattingService.formatDate(reservation.checkOut);

    MessageRepository.save("EMAIL", HOUSEKEEPING_EMAIL, subject, body);
  }
}

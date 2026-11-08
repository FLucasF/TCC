package com.pousada.marealta.notification;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.ReservationStatus;
import com.pousada.marealta.repository.MessageRepository;
import com.pousada.marealta.service.ReceiptFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class GuestNotificationObserver implements ReservationObserver {
  private static final Logger logger = LoggerFactory.getLogger(GuestNotificationObserver.class);

  @Override
  public void onReservationConfirmed(Reservation reservation) {
    try {
      String receipt = ReceiptFormatter.formatReceipt(reservation);
      String body = "Olá, " + reservation.getGuestName() + "!\n\n" +
          "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
          receipt + "\n\n" +
          "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
          "Até breve!";

      MessageRepository.save("EMAIL", reservation.getGuestEmail(),
          "Reserva confirmada - Pousada Maré Alta", body);
      logger.info("Guest notification sent for reservation {}", reservation.getId());
    } catch (SQLException e) {
      logger.error("Failed to send guest notification", e);
    }
  }

  @Override
  public void onReservationCancelled(Reservation reservation) {
  }
}

package com.pousada.marealta.notification;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.repository.MessageRepository;
import com.pousada.marealta.service.ReceiptFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class GovernanceNotificationObserver implements ReservationObserver {
  private static final Logger logger = LoggerFactory.getLogger(GovernanceNotificationObserver.class);
  private static final String GOVERNANCE_EMAIL = "governanca@pousadamarealta.com.br";

  @Override
  public void onReservationConfirmed(Reservation reservation) {
    try {
      String checkInStr = ReceiptFormatter.formatDate(reservation.getCheckIn());
      String checkOutStr = ReceiptFormatter.formatDate(reservation.getCheckOut());
      String guestsLabel = reservation.getGuests() == 1 ? "1 hóspede" : reservation.getGuests() + " hóspedes";

      String subject = "Reserva confirmada - quarto " + reservation.getRoomNumber();
      String body = "Quarto " + reservation.getRoomNumber() + "\n" +
          "Entrada: " + checkInStr + "\n" +
          "Saída: " + checkOutStr + "\n" +
          "Hóspedes: " + guestsLabel;

      MessageRepository.save("EMAIL", GOVERNANCE_EMAIL, subject, body);
      logger.info("Governance notification sent for reservation {}", reservation.getId());
    } catch (SQLException e) {
      logger.error("Failed to send governance notification", e);
    }
  }

  @Override
  public void onReservationCancelled(Reservation reservation) {
    try {
      String checkInStr = ReceiptFormatter.formatDate(reservation.getCheckIn());
      String checkOutStr = ReceiptFormatter.formatDate(reservation.getCheckOut());

      String subject = "Reserva cancelada - quarto " + reservation.getRoomNumber();
      String body = "Quarto " + reservation.getRoomNumber() + " liberado:\n" +
          "Entrada: " + checkInStr + "\n" +
          "Saída: " + checkOutStr;

      MessageRepository.save("EMAIL", GOVERNANCE_EMAIL, subject, body);
      logger.info("Governance cancellation notification sent for reservation {}", reservation.getId());
    } catch (SQLException e) {
      logger.error("Failed to send governance cancellation notification", e);
    }
  }
}

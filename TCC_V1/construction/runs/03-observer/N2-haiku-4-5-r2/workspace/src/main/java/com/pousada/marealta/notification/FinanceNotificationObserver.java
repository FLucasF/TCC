package com.pousada.marealta.notification;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.ReservationStatus;
import com.pousada.marealta.repository.MessageRepository;
import com.pousada.marealta.service.ReceiptFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;

public class FinanceNotificationObserver implements ReservationObserver {
  private static final Logger logger = LoggerFactory.getLogger(FinanceNotificationObserver.class);
  private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

  @Override
  public void onReservationConfirmed(Reservation reservation) {
    try {
      String amountStr = ReceiptFormatter.formatMoney(reservation.getTotal());
      String subject = "Pagamento recebido - reserva " + reservation.getId();
      String body = "Valor recebido: " + amountStr;

      MessageRepository.save("EMAIL", FINANCE_EMAIL, subject, body);
      logger.info("Finance notification sent for reservation {}", reservation.getId());
    } catch (SQLException e) {
      logger.error("Failed to send finance notification", e);
    }
  }

  @Override
  public void onReservationCancelled(Reservation reservation) {
    try {
      BigDecimal refund = reservation.getRefund();
      if (refund == null || refund.compareTo(BigDecimal.ZERO) <= 0) {
        logger.info("No refund to notify for reservation {}", reservation.getId());
        return;
      }

      String refundStr = ReceiptFormatter.formatMoney(refund);
      String subject = "Reembolso - reserva " + reservation.getId();
      String body = "Valor a reembolsar: " + refundStr;

      MessageRepository.save("EMAIL", FINANCE_EMAIL, subject, body);
      logger.info("Finance cancellation notification sent for reservation {}", reservation.getId());
    } catch (SQLException e) {
      logger.error("Failed to send finance cancellation notification", e);
    }
  }
}

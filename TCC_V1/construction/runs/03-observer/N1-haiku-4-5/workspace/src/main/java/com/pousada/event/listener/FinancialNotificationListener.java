package com.pousada.event.listener;

import com.pousada.database.MessageRepository;
import com.pousada.event.ReservationCancelledEvent;
import com.pousada.event.ReservationConfirmedEvent;
import com.pousada.event.ReservationEventListener;
import com.pousada.model.Reservation;
import com.pousada.service.FormattingService;
import java.math.BigDecimal;

public class FinancialNotificationListener implements ReservationEventListener {
  private static final String FINANCIAL_EMAIL = "financeiro@pousadamarealta.com.br";

  @Override
  public void onReservationConfirmed(ReservationConfirmedEvent event) {
    Reservation reservation = event.getReservation();
    String subject = "Pagamento recebido - reserva " + reservation.id;
    String body = "Valor: " + FormattingService.formatCurrency(event.getAmountPaid());

    MessageRepository.save("EMAIL", FINANCIAL_EMAIL, subject, body);
  }

  @Override
  public void onReservationCancelled(ReservationCancelledEvent event) {
    BigDecimal refund = event.getRefund();

    if (refund == null || refund.compareTo(BigDecimal.ZERO) <= 0) {
      return;
    }

    Reservation reservation = event.getReservation();
    String subject = "Reembolso - reserva " + reservation.id;
    String body = "Valor: " + FormattingService.formatCurrency(refund);

    MessageRepository.save("EMAIL", FINANCIAL_EMAIL, subject, body);
  }
}

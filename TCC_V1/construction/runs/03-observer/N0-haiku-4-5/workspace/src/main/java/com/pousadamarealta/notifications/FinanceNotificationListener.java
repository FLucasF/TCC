package com.pousadamarealta.notifications;

import com.pousadamarealta.models.Reservation;
import com.pousadamarealta.models.ReservationStatus;
import com.pousadamarealta.utils.PriceFormatter;
import com.pousadamarealta.MessageService;
import java.math.BigDecimal;

public class FinanceNotificationListener implements NotificationListener {
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        String subject = "Pagamento recebido - reserva " + reservation.id;
        String body = String.format(
            "Reserva nº %d\nValor: %s",
            reservation.id,
            PriceFormatter.format(reservation.total)
        );

        MessageService.sendMessage("EMAIL", FINANCE_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, BigDecimal refundAmount) {
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        String subject = "Reembolso - reserva " + reservation.id;
        String body = String.format(
            "Reserva nº %d\nValor a devolver: %s",
            reservation.id,
            PriceFormatter.format(refundAmount)
        );

        MessageService.sendMessage("EMAIL", FINANCE_EMAIL, subject, body);
    }
}

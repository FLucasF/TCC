package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class FinanceNotificationHandler implements NotificationHandler {
    private static final String EMAIL = "financeiro@pousadamarealta.com.br";
    private long messageIdCounter = 1;

    @Override
    public List<NotificationMessage> handle(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent confirmed) {
            return List.of(createPaymentEmail(confirmed.reservation()));
        } else if (event instanceof ReservationCancelledEvent cancelled) {
            if (cancelled.previousStatus() == ReservationStatus.CONFIRMED &&
                cancelled.reservation().getRefund() != null &&
                cancelled.reservation().getRefund().compareTo(BigDecimal.ZERO) > 0) {
                return List.of(createRefundEmail(cancelled.reservation()));
            }
        }
        return Collections.emptyList();
    }

    private NotificationMessage createPaymentEmail(Reservation reservation) {
        String subject = "Pagamento recebido - reserva " + reservation.getId();
        String body = String.format(
            "Pagamento recebido.\nReserva: %d\nValor: %s",
            reservation.getId(),
            formatMoney(reservation.getAmountPaid())
        );

        return new NotificationMessage(messageIdCounter++, "EMAIL", EMAIL, subject, body);
    }

    private NotificationMessage createRefundEmail(Reservation reservation) {
        String subject = "Reembolso - reserva " + reservation.getId();
        String body = String.format(
            "Reembolso necessário.\nReserva: %d\nValor: %s",
            reservation.getId(),
            formatMoney(reservation.getRefund())
        );

        return new NotificationMessage(messageIdCounter++, "EMAIL", EMAIL, subject, body);
    }

    private String formatMoney(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        return "R$ " + format.format(amount);
    }

    public void setMessageIdCounter(long counter) {
        this.messageIdCounter = counter;
    }
}

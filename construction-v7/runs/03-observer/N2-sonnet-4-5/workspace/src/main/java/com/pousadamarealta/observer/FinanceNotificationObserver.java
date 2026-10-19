package com.pousadamarealta.observer;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.service.EmailService;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class FinanceNotificationObserver implements ReservationObserver {
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    private final EmailService emailService;

    public FinanceNotificationObserver(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public void onReservationEvent(ReservationEvent event) {
        switch (event.getType()) {
            case CONFIRMED -> sendPaymentNotification(event.getReservation());
            case CANCELLED -> {
                if (event.getPreviousState() != null &&
                    event.getPreviousState().getStatus() == ReservationStatus.CONFIRMED) {
                    BigDecimal refundAmount = event.getReservation().getRefund();
                    if (refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                        sendRefundNotification(event.getReservation(), refundAmount);
                    }
                }
            }
        }
    }

    private void sendPaymentNotification(Reservation reservation) {
        String subject = String.format("Pagamento recebido - reserva %d", reservation.getId());
        String body = String.format("Valor: %s", formatMoney(reservation.getAmountPaid()));
        emailService.send(FINANCE_EMAIL, subject, body);
    }

    private void sendRefundNotification(Reservation reservation, BigDecimal amount) {
        String subject = String.format("Reembolso - reserva %d", reservation.getId());
        String body = String.format("Valor a devolver: %s", formatMoney(amount));
        emailService.send(FINANCE_EMAIL, subject, body);
    }

    private String formatMoney(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("R$ #,##0.00", symbols);
        return format.format(amount);
    }
}

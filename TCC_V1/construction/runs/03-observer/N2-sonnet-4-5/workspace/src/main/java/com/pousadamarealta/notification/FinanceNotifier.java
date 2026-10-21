package com.pousadamarealta.notification;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;
import java.util.Locale;

public final class FinanceNotifier implements ReservationListener {
    private static final String EMAIL = "financeiro@pousadamarealta.com.br";
    private final MessageSender messageSender;

    public FinanceNotifier(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public void onEvent(ReservationEvent event) {
        switch (event) {
            case ReservationEvent.Confirmed confirmed ->
                notifyPayment(confirmed.reservation());
            case ReservationEvent.Cancelled cancelled when cancelled.wasConfirmed() && hasRefund(cancelled.reservation()) ->
                notifyRefund(cancelled.reservation());
            default -> {}
        }
    }

    private boolean hasRefund(Reservation reservation) {
        return reservation.refund() != null && reservation.refund().compareTo(BigDecimal.ZERO) > 0;
    }

    private void notifyPayment(Reservation reservation) {
        String body = String.format("Valor: %s", formatMoney(reservation.amountPaid()));

        messageSender.send(
            MessageChannel.EMAIL,
            EMAIL,
            String.format("Pagamento recebido - reserva %d", reservation.id()),
            body
        );
    }

    private void notifyRefund(Reservation reservation) {
        String body = String.format("Valor: %s", formatMoney(reservation.refund()));

        messageSender.send(
            MessageChannel.EMAIL,
            EMAIL,
            String.format("Reembolso - reserva %d", reservation.id()),
            body
        );
    }

    private String formatMoney(BigDecimal amount) {
        return String.format(Locale.of("pt", "BR"), "R$ %,.2f", amount);
    }
}

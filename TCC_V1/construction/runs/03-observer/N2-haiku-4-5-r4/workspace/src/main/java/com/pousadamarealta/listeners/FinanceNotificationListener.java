package com.pousadamarealta.listeners;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.events.ReservationEventListener;
import com.pousadamarealta.services.MessageService;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.utils.Formatting;
import java.math.BigDecimal;

public class FinanceNotificationListener implements ReservationEventListener {
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";
    private final MessageService messageService;

    public FinanceNotificationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        String subject = String.format("Pagamento recebido - reserva %d", reservation.getId());
        String body = String.format("Valor: %s", Formatting.formatMoney(reservation.getTotal()));

        messageService.sendMessage(MessageChannel.EMAIL, FINANCE_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
        if (reservation.getRefund() != null && reservation.getRefund().signum() > 0) {
            String subject = String.format("Reembolso - reserva %d", reservation.getId());
            String body = String.format("Valor: %s", Formatting.formatMoney(reservation.getRefund()));

            messageService.sendMessage(MessageChannel.EMAIL, FINANCE_EMAIL, subject, body);
        }
    }
}

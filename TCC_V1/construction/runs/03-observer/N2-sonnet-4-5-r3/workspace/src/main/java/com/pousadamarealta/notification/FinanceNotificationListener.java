package com.pousadamarealta.notification;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.util.Formatter;

import java.math.BigDecimal;

public final class FinanceNotificationListener implements ReservationEventListener {
    private final MessageService messageService;

    public FinanceNotificationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, RoomType roomType) {
        String body = Formatter.formatMoney(reservation.amountPaid());

        messageService.send(
            MessageChannel.EMAIL,
            "financeiro@pousadamarealta.com.br",
            String.format("Pagamento recebido - reserva %d", reservation.id()),
            body
        );
    }

    @Override
    public void onReservationCancelled(Reservation reservation, RoomType roomType, boolean wasPreviouslyConfirmed) {
        if (!wasPreviouslyConfirmed || reservation.refund().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        String body = Formatter.formatMoney(reservation.refund());

        messageService.send(
            MessageChannel.EMAIL,
            "financeiro@pousadamarealta.com.br",
            String.format("Reembolso - reserva %d", reservation.id()),
            body
        );
    }
}

package com.pousadamarealta.event;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.service.MessageService;

import java.math.BigDecimal;

public class FinanceConfirmationHandler implements EventHandler<ReservationConfirmedEvent> {
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    private final MessageService messageService;

    public FinanceConfirmationHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation r = event.getReservation();

        String subject = String.format("Pagamento recebido - reserva %d", r.getId());
        String body = String.format("Valor pago: %s", formatMoney(r.getAmountPaid()));

        messageService.send(MessageChannel.EMAIL, FINANCE_EMAIL, subject, body);
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }
}

package com.pousadamarealta.service.listeners;

import com.pousadamarealta.domain.events.EventListener;
import com.pousadamarealta.domain.events.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.util.Formatter;

public class FinanceNotificationListener implements
    EventListener<ReservationConfirmedEvent> {

    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";
    private final MessageService messageService;

    public FinanceNotificationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation r = event.getReservation();

        String subject = "Pagamento recebido - reserva " + r.getId();
        String body = String.format(
            "Um pagamento foi recebido:\n\n" +
            "Reserva: %d\n" +
            "Valor: %s",
            r.getId(),
            Formatter.formatMoney(r.getAmountPaid())
        );

        messageService.sendEmail(FINANCE_EMAIL, subject, body);
    }
}

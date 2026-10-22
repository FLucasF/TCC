package com.pousadamarealta.service.event.listeners;

import com.pousadamarealta.domain.events.ReservationCancelledEvent;
import com.pousadamarealta.domain.events.ReservationConfirmedEvent;
import com.pousadamarealta.domain.events.ReservationEvent;
import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.service.event.EventListener;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class FinanceNotificationListener implements EventListener {
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";
    private static final DecimalFormat MONEY_FORMATTER;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        MONEY_FORMATTER = new DecimalFormat("#,##0.00", symbols);
    }

    private final MessageRepository messageRepository;

    public FinanceNotificationListener(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void handle(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent) {
            notifyPayment((ReservationConfirmedEvent) event);
        } else if (event instanceof ReservationCancelledEvent cancelledEvent) {
            if (cancelledEvent.wasConfirmed() && hasRefund(cancelledEvent.getReservation())) {
                notifyRefund(cancelledEvent);
            }
        }
    }

    private void notifyPayment(ReservationConfirmedEvent event) {
        Reservation reservation = event.getReservation();

        String subject = String.format("Pagamento recebido - reserva %d", reservation.getId());
        String body = formatMoney(reservation.getAmountPaid());

        Message message = Message.email(FINANCE_EMAIL, subject, body);
        messageRepository.save(message);
    }

    private void notifyRefund(ReservationCancelledEvent event) {
        Reservation reservation = event.getReservation();

        String subject = String.format("Reembolso - reserva %d", reservation.getId());
        String body = formatMoney(reservation.getRefund());

        Message message = Message.email(FINANCE_EMAIL, subject, body);
        messageRepository.save(message);
    }

    private boolean hasRefund(Reservation reservation) {
        return reservation.getRefund() != null &&
               reservation.getRefund().compareTo(BigDecimal.ZERO) > 0;
    }

    private String formatMoney(BigDecimal amount) {
        return "R$ " + MONEY_FORMATTER.format(amount);
    }
}

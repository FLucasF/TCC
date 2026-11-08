package com.pousadamarealta.notification;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.util.MoneyUtils;
import com.pousadamarealta.model.Message;

public class FinanceNotifier implements ReservationNotifier {
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";
    private final MessageRepository messageRepository;

    public FinanceNotifier(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) throws Exception {
        String subject = String.format("Pagamento recebido - reserva %d", reservation.getId());
        String body = String.format(
                "Valor: %s",
                MoneyUtils.formatMoneyPrecise(reservation.getTotal())
        );

        Message message = new Message(0, "EMAIL", FINANCE_EMAIL, subject, body);
        messageRepository.save(message);
    }

    @Override
    public void onReservationCancelled(Reservation reservation) throws Exception {
        if (reservation.getRefund() == null || reservation.getRefund().signum() == 0) {
            return;
        }

        String subject = String.format("Reembolso - reserva %d", reservation.getId());
        String body = String.format(
                "Valor: %s",
                MoneyUtils.formatMoneyPrecise(reservation.getRefund())
        );

        Message message = new Message(0, "EMAIL", FINANCE_EMAIL, subject, body);
        messageRepository.save(message);
    }
}

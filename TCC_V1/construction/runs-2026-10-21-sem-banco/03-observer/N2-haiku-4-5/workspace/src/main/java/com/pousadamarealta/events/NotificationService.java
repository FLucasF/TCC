package com.pousadamarealta.events;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.repository.MessageRepository;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class NotificationService {
    private final MessageRepository messageRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter CURRENCY_FORMATTER = null;

    public NotificationService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void notifyGovernanceOnConfirmation(Reservation reservation) {
        String subject = "Reserva confirmada - quarto " + reservation.getRoomNumber();
        String body = String.format(
            "Entrada: %s | Saída: %s | %d hóspede%s",
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER),
            reservation.getGuests(),
            reservation.getGuests() > 1 ? "s" : ""
        );

        Message message = new Message(
            messageRepository.getNextMessageId(),
            Message.MessageChannel.EMAIL,
            "governanca@pousadamarealta.com.br",
            subject,
            body
        );
        messageRepository.save(message);
    }

    public void notifyFinanceOnConfirmation(Reservation reservation) {
        String subject = "Pagamento recebido - reserva " + reservation.getId();
        String body = "Valor pago: " + formatCurrency(reservation.getTotal());

        Message message = new Message(
            messageRepository.getNextMessageId(),
            Message.MessageChannel.EMAIL,
            "financeiro@pousadamarealta.com.br",
            subject,
            body
        );
        messageRepository.save(message);
    }

    public void notifyGovernanceOnCancellation(Reservation reservation) {
        String subject = "Reserva cancelada - quarto " + reservation.getRoomNumber();
        String body = String.format(
            "Datas liberadas: %s a %s",
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER)
        );

        Message message = new Message(
            messageRepository.getNextMessageId(),
            Message.MessageChannel.EMAIL,
            "governanca@pousadamarealta.com.br",
            subject,
            body
        );
        messageRepository.save(message);
    }

    public void notifyFinanceOnCancellation(Reservation reservation) {
        if (reservation.getRefund() != null && reservation.getRefund().compareTo(BigDecimal.ZERO) > 0) {
            String subject = "Reembolso - reserva " + reservation.getId();
            String body = "Valor a devolver: " + formatCurrency(reservation.getRefund());

            Message message = new Message(
                messageRepository.getNextMessageId(),
                Message.MessageChannel.EMAIL,
                "financeiro@pousadamarealta.com.br",
                subject,
                body
            );
            messageRepository.save(message);
        }
    }

    private String formatCurrency(BigDecimal value) {
        return String.format("R$ %,.2f", value).replace(",", "X").replace(".", ",").replace("X", ".");
    }
}

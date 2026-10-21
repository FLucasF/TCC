package com.pousadamarealta.service;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.MessageRepository;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class NotificationService {
    private final MessageRepository messageRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public NotificationService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendConfirmationEmail(Reservation reservation, String receiptText) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.getGuestName(),
            receiptText
        );

        Message message = new Message(
            messageRepository.nextId(),
            "EMAIL",
            reservation.getGuestEmail(),
            subject,
            body
        );
        messageRepository.save(message);
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}

package com.pousadamarealta.service;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.repository.MessageRepository;

public class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendEmail(String to, String subject, String body) {
        Message message = new Message("EMAIL", to, subject, body);
        messageRepository.save(message);
    }

    public String createConfirmationEmail(String guestName, String receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(guestName).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(receipt).append("\n\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }
}

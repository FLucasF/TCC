package com.pousadamareal.service;

import com.pousadamareal.model.Document;
import com.pousadamareal.model.Message;
import com.pousadamareal.repository.MessageRepository;

import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendConfirmationEmail(String guestName, String email, Document document) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """, guestName, document.getText());

        messageRepository.save("EMAIL", email, subject, body);
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}

package br.com.pousada.maralta.service;

import br.com.pousada.maralta.domain.Document;
import br.com.pousada.maralta.domain.Message;
import br.com.pousada.maralta.repository.MessageRepository;

import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendConfirmationEmail(String guestName, String guestEmail, Document document) {
        String subject = "Reserva confirmada - Pousada Maré Alta";

        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """, guestName, document.getText());

        messageRepository.save("EMAIL", guestEmail, subject, body);
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}

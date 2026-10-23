package br.com.pousada.marealta.service;

import br.com.pousada.marealta.model.Document;
import br.com.pousada.marealta.model.Message;
import br.com.pousada.marealta.repository.MessageRepository;

public class EmailService {
    private final MessageRepository messageRepository;

    public EmailService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendConfirmationEmail(String guestEmail, String guestName, Document document) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildEmailBody(guestName, document);

        Message message = new Message();
        message.setChannel("EMAIL");
        message.setTo(guestEmail);
        message.setSubject(subject);
        message.setBody(body);

        messageRepository.create(message);
    }

    private String buildEmailBody(String guestName, Document document) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(guestName).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(document.getText()).append("\n\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }
}

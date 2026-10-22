package br.com.marealte.service;

import br.com.marealte.model.Document;
import br.com.marealte.model.Message;
import br.com.marealte.repository.MessageRepository;

public class EmailService {

    private final MessageRepository messageRepository;

    public EmailService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendConfirmationEmail(String guestName, String guestEmail, Document document) {
        String subject = "Reserva confirmada - Pousada Maré Alta";

        StringBuilder body = new StringBuilder();
        body.append("Olá, ").append(guestName).append("!\n\n");
        body.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        body.append(document.getText()).append("\n\n");
        body.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        body.append("Até breve!");

        Message message = new Message("EMAIL", guestEmail, subject, body.toString());
        messageRepository.save(message);
    }
}

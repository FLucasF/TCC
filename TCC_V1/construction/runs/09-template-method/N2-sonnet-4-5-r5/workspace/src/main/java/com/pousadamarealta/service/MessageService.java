package com.pousadamarealta.service;

import com.pousadamarealta.domain.Document;
import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.repository.MessageRepository;

public class MessageService {
    private final MessageRepository messageRepository;
    private final DocumentService documentService;

    public MessageService(MessageRepository messageRepository, DocumentService documentService) {
        this.messageRepository = messageRepository;
        this.documentService = documentService;
    }

    public void sendConfirmationEmail(String recipientEmail, String guestName, Document document) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationEmailBody(guestName, document);

        Message message = new Message();
        message.setChannel(MessageChannel.EMAIL);
        message.setTo(recipientEmail);
        message.setSubject(subject);
        message.setBody(body);

        messageRepository.save(message);
    }

    private String buildConfirmationEmailBody(String guestName, Document document) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(guestName).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(documentService.formatDocument(document)).append("\n\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");

        return sb.toString();
    }
}

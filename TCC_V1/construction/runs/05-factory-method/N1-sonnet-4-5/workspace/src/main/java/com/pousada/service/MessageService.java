package com.pousada.service;

import com.pousada.domain.Document;
import com.pousada.domain.Message;
import com.pousada.domain.Reservation;
import com.pousada.repository.MessageRepository;

import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;
    private final DocumentService documentService;

    public MessageService(MessageRepository messageRepository, DocumentService documentService) {
        this.messageRepository = messageRepository;
        this.documentService = documentService;
    }

    public void sendConfirmationEmail(Reservation reservation) {
        Document document = documentService.generateDocument(reservation);

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationEmail(reservation.getGuestName(), document.getText());

        messageRepository.save("EMAIL", reservation.getGuestEmail(), subject, body);
    }

    private String buildConfirmationEmail(String guestName, String documentText) {
        return String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            guestName, documentText
        );
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}

package com.pousada.service;

import com.pousada.domain.Message;
import com.pousada.domain.Reservation;
import com.pousada.repository.MessageRepository;

public class EmailService {
    private final MessageRepository messageRepository;
    private final DocumentService documentService;

    public EmailService(MessageRepository messageRepository, DocumentService documentService) {
        this.messageRepository = messageRepository;
        this.documentService = documentService;
    }

    public void sendConfirmationEmail(Reservation reservation) {
        String document = documentService.generateDocument(reservation);

        String body = String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.getGuestName(),
            document
        );

        long id = messageRepository.nextId();
        Message message = new Message(
            id,
            "EMAIL",
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );

        messageRepository.save(message);
    }

    public java.util.List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}

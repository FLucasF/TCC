package com.pousada.service;

import com.pousada.domain.Message;
import com.pousada.domain.MessageChannel;
import com.pousada.domain.Reservation;
import com.pousada.repository.MessageRepository;

import java.util.List;

public class MessageService {
    private final MessageRepository messageRepository;
    private final DocumentGenerator documentGenerator;

    public MessageService(MessageRepository messageRepository, DocumentGenerator documentGenerator) {
        this.messageRepository = messageRepository;
        this.documentGenerator = documentGenerator;
    }

    public void sendConfirmationEmail(Reservation reservation) {
        var document = documentGenerator.generateDocument(reservation);
        var documentText = documentGenerator.formatDocument(document);

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", reservation.getGuestName(), documentText);

        Message message = new Message(
            null,
            MessageChannel.EMAIL,
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

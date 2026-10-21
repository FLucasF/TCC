package com.pousadamarealta.service;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.repository.MessageRepository;

public class EmailService {
    private final MessageRepository messageRepository;
    private final DocumentService documentService;

    public EmailService(MessageRepository messageRepository, DocumentService documentService) {
        this.messageRepository = messageRepository;
        this.documentService = documentService;
    }

    public void sendConfirmationEmail(Reservation reservation) {
        String receipt = documentService.generateReceipt(reservation);

        String body = String.format("""
                Olá, %s!

                Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                %s

                A entrada é a partir das 14h, e a saída, até as 12h.
                Até breve!""", reservation.getGuestName(), receipt);

        messageRepository.save("EMAIL", reservation.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta", body);
    }
}

package com.marealta.service;

import com.marealta.db.MessageRepository;
import com.marealta.model.Message;
import com.marealta.model.Reservation;

public class EmailService {
    private final MessageRepository messageRepository;
    private final ReceiptService receiptService;

    public EmailService(MessageRepository messageRepository, ReceiptService receiptService) {
        this.messageRepository = messageRepository;
        this.receiptService = receiptService;
    }

    public void sendConfirmationEmail(Reservation reservation) {
        String receipt = receiptService.generateReceipt(reservation);
        String body = buildConfirmationEmailBody(reservation, receipt);

        Message message = new Message();
        message.setChannel("EMAIL");
        message.setTo(reservation.getGuestEmail());
        message.setSubject("Reserva confirmada - Pousada Maré Alta");
        message.setBody(body);

        messageRepository.save(message);
    }

    private String buildConfirmationEmailBody(Reservation reservation, String receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(receipt).append("\n\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");

        return sb.toString();
    }
}

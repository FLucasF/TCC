package com.pousada.marealta.service;

import com.pousada.marealta.dto.DocumentResponse;
import com.pousada.marealta.model.Message;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.repository.MessageRepository;

import java.sql.SQLException;

public class MessageService {

    public static void sendConfirmationEmail(Reservation reservation) throws SQLException {
        DocumentResponse doc = DocumentService.generateDocument(reservation);

        String emailBody = buildConfirmationEmail(reservation, doc);

        Message message = new Message();
        message.setChannel(Message.Channel.EMAIL);
        message.setTo(reservation.getGuestEmail());
        message.setSubject("Reserva confirmada - Pousada Maré Alta");
        message.setBody(emailBody);

        MessageRepository.create(message);
    }

    private static String buildConfirmationEmail(Reservation reservation, DocumentResponse doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(doc.text).append("\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!\n");
        return sb.toString();
    }
}

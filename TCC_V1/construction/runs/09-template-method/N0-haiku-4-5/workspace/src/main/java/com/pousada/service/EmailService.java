package com.pousada.service;

import com.pousada.model.Message;
import com.pousada.model.Reservation;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.RoomRepository;

public class EmailService {

    public static void sendConfirmationEmail(Reservation reservation) {
        String receipt = DocumentService.generateReceipt(reservation);
        String body = String.format("""
                Olá, %s!

                Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                %s

                A entrada é a partir das 14h, e a saída, até as 12h.
                Até breve!
                """, reservation.getGuestName(), receipt);

        Message message = new Message("EMAIL", reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta", body);
        MessageRepository.create(message);
    }
}

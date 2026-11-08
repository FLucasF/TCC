package com.marealata.service;

import com.marealata.dao.MessageDao;
import com.marealata.dao.RoomDao;
import com.marealata.model.*;
import com.marealata.util.DocumentFormatter;
import java.sql.SQLException;

public class MessageService {
    public static void sendConfirmationEmail(Reservation reservation, String roomType) throws SQLException {
        String documentText = DocumentFormatter.formatReservationDocument(reservation, roomType);

        String body = """
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """.formatted(reservation.guestName, documentText);

        Message message = new Message("EMAIL", reservation.guestEmail,
                                     "Reserva confirmada - Pousada Maré Alta", body);

        MessageDao.save(message);
    }
}

package com.pousada.marealta.service;

import com.pousada.marealta.model.Message;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.repository.MessageRepository;
import com.pousada.marealta.repository.RoomRepository;
import com.pousada.marealta.util.DocumentFormatter;

public class MessageService {

    public static void sendConfirmationEmail(Reservation res) throws Exception {
        var room = RoomRepository.findById(res.getRoomNumber())
            .orElseThrow(() -> new RuntimeException("Room not found"));

        String documentText = getDocumentText(res, room.type());

        String body = String.format(
            """
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """,
            res.getGuestName(),
            documentText
        );

        Message msg = new Message();
        msg.setChannel("EMAIL");
        msg.setTo(res.getGuestEmail());
        msg.setSubject("Reserva confirmada - Pousada Maré Alta");
        msg.setBody(body);

        MessageRepository.save(msg);
    }

    private static String getDocumentText(Reservation res, String roomType) {
        if (res.getGuestCpf() != null) {
            return DocumentFormatter.formatComprovante(res, roomType);
        } else if (res.getCompanyCnpj() != null) {
            return DocumentFormatter.formatFatura(res, roomType);
        } else if (res.getGuestPassport() != null) {
            return DocumentFormatter.formatBookingReceipt(res, roomType);
        }
        return "";
    }
}

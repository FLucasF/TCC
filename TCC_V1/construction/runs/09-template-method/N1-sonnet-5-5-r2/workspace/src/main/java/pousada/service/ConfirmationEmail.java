package pousada.service;

import pousada.domain.Establishment;
import pousada.domain.Message;
import pousada.domain.MessageChannel;
import pousada.domain.Receipt;
import pousada.domain.Reservation;

final class ConfirmationEmail {
    static final String SUBJECT = "Reserva confirmada - " + Establishment.NAME;

    private ConfirmationEmail() {
    }

    static Message build(Reservation reservation, Receipt receipt) {
        String body = "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na " + Establishment.NAME + " está confirmada. Estes são os dados dela:\n\n"
                + ReceiptText.format(receipt) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return new Message(null, MessageChannel.EMAIL, reservation.guestEmail(), SUBJECT, body);
    }
}

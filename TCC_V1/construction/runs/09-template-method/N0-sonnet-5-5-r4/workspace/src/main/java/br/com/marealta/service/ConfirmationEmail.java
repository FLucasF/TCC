package br.com.marealta.service;

import br.com.marealta.domain.Channel;
import br.com.marealta.domain.Message;
import br.com.marealta.domain.Pousada;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;

final class ConfirmationEmail {
    private ConfirmationEmail() {
    }

    static Message compose(Reservation reservation, Receipt receipt) {
        String body = "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na " + Pousada.NAME + " está confirmada. Estes são os dados dela:\n\n"
                + receipt.text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return new Message(null, Channel.EMAIL, reservation.guestEmail(),
                "Reserva confirmada - " + Pousada.NAME, body);
    }
}

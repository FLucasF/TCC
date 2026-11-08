package com.pousada.service;

import com.pousada.domain.Channel;
import com.pousada.domain.Message;
import com.pousada.domain.Receipt;

public final class ConfirmationEmail {
    private ConfirmationEmail() {
    }

    public static Message of(String guestName, String guestEmail, Receipt receipt) {
        String body = "Olá, " + guestName + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt.format() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return new Message(null, Channel.EMAIL, guestEmail, "Reserva confirmada - Pousada Maré Alta", body);
    }
}

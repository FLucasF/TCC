package br.com.marealta.mail;

import br.com.marealta.domain.Receipt;

public final class ConfirmationEmail {
    private ConfirmationEmail() {
    }

    public static Message to(String address, String guestName, Receipt receipt) {
        String body = "Olá, " + guestName + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt.text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return Message.email(address, "Reserva confirmada - Pousada Maré Alta", body);
    }
}

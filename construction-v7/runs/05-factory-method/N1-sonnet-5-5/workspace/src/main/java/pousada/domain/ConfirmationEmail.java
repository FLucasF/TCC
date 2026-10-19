package pousada.domain;

import pousada.domain.document.Document;

/** E-mail de confirmação: sempre em português, só o documento do meio muda. */
final class ConfirmationEmail {

    static final String SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private ConfirmationEmail() {
    }

    static String body(String guestName, Document document) {
        return "Olá, " + guestName + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + document.text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}

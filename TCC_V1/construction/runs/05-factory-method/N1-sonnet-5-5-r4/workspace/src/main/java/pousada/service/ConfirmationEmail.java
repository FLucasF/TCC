package pousada.service;

/** E-mail de confirmação: sempre em português; muda só o documento que vai dentro dele. */
final class ConfirmationEmail {
    static final String SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private ConfirmationEmail() {
    }

    static String body(String guestName, String documentText) {
        return "Olá, " + guestName + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + documentText + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}

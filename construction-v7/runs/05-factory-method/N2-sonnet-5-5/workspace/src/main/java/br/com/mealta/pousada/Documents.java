package br.com.mealta.pousada;

public final class Documents {
    private Documents() {}

    public static String text(Reservation r) {
        return r.identity().render(r.documentData());
    }

    /** O e-mail é sempre em português; só o documento muda. */
    public static String confirmationEmail(Reservation r) {
        return "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + text(r) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}

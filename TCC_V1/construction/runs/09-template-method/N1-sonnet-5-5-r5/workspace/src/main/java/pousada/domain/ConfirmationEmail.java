package pousada.domain;

public final class ConfirmationEmail {
    public static final String SUBJECT = "Reserva confirmada - " + Pousada.NAME;

    private ConfirmationEmail() {
    }

    public static String body(Receipt receipt) {
        return "Olá, " + receipt.guestName() + "!\n\n"
                + "Sua reserva na " + Pousada.NAME + " está confirmada. Estes são os dados dela:\n\n"
                + ReceiptText.of(receipt) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}

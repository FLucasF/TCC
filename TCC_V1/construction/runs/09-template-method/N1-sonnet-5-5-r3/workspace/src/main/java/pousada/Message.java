package pousada;

public record Message(Long id, Channel channel, String to, String subject, String body) {
    public enum Channel {
        EMAIL
    }

    private static final String CONFIRMATION_SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    public static Message confirmation(Reservation reservation, Receipt receipt) {
        String body = "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt.toText() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return new Message(null, Channel.EMAIL, reservation.guestEmail(), CONFIRMATION_SUBJECT, body);
    }
}

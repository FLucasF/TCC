package br.com.mare.pousada.domain;

public record Message(Long id, MessageChannel channel, String to, String subject, String body) {

    public static Message reservationConfirmed(Reservation reservation, Receipt receipt) {
        String body = "Olá, " + reservation.getGuestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt.text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return new Message(null, MessageChannel.EMAIL, reservation.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta", body);
    }
}

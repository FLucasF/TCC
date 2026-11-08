package pousada.service;

import pousada.domain.Channel;
import pousada.domain.Receipt;
import pousada.domain.Reservation;
import pousada.repo.MessageRepository;

/** Manda ao hóspede o e-mail de reserva confirmada (por ora, só guarda na lista de enviadas). */
public class ConfirmationMailer {
    static final String SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private final MessageRepository messages;

    public ConfirmationMailer(MessageRepository messages) {
        this.messages = messages;
    }

    public void send(Reservation r) {
        String body = "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Receipt.of(r).text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        messages.add(Channel.EMAIL, r.guestEmail(), SUBJECT, body);
    }
}

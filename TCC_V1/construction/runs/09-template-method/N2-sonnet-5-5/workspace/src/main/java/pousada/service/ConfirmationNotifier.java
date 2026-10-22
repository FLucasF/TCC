package pousada.service;

import pousada.db.MessageRepository;
import pousada.domain.Message;
import pousada.domain.Receipt;
import pousada.domain.Reservation;

/** Avisa o hóspede da confirmação; por enquanto o e-mail só fica guardado na lista de enviados. */
public class ConfirmationNotifier {
    static final String SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private final MessageRepository messages;

    public ConfirmationNotifier(MessageRepository messages) {
        this.messages = messages;
    }

    public void confirmed(Reservation reservation, Receipt receipt) {
        String body = "Olá, " + reservation.getGuestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt.text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        messages.save(new Message(null, Message.Channel.EMAIL, reservation.getGuestEmail(), SUBJECT, body));
    }
}

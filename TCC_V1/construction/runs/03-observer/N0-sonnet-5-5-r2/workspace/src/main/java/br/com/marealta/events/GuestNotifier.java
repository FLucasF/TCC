package br.com.marealta.events;

import br.com.marealta.domain.OutgoingMessage;
import br.com.marealta.domain.Receipt;

/** E-mail de confirmação para o hóspede, com o comprovante. */
public final class GuestNotifier implements ReservationListener {
    private final MessageSender sender;

    public GuestNotifier(MessageSender sender) {
        this.sender = sender;
    }

    @Override
    public void onConfirmed(ReservationEvent.Confirmed e) {
        String body = "Olá, " + e.reservation().guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Receipt.of(e.reservation(), e.room()).toText() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        sender.send(OutgoingMessage.email(e.reservation().guestEmail(),
                "Reserva confirmada - Pousada Maré Alta", body));
    }
}

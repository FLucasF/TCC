package br.com.marealta.notification;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;

/** Avisa a governança para preparar o quarto, ou que ele ficou livre. */
public final class HousekeepingNotifier implements ReservationListener {

    private final MessageSender sender;
    private final String address;

    public HousekeepingNotifier(MessageSender sender, String address) {
        this.sender = sender;
        this.address = address;
    }

    @Override
    public void onConfirmed(Reservation r) {
        String body = "O quarto " + r.room().number() + " (" + r.room().type() + ") foi reservado.\n"
                + "Entrada: " + Money.format(r.checkInDate()) + "\n"
                + "Saída: " + Money.format(r.checkOutDate()) + "\n"
                + "Hóspedes: " + r.guests();
        sender.send(Message.email(address, "Reserva confirmada - quarto " + r.room().number(), body));
    }

    @Override
    public void onCancelled(Reservation r) {
        if (!r.hasPayment()) {
            return;
        }
        String body = "A reserva nº " + r.id() + " do quarto " + r.room().number() + " foi cancelada.\n"
                + "Entrada que ficou livre: " + Money.format(r.checkInDate()) + "\n"
                + "Saída que ficou livre: " + Money.format(r.checkOutDate());
        sender.send(Message.email(address, "Reserva cancelada - quarto " + r.room().number(), body));
    }
}

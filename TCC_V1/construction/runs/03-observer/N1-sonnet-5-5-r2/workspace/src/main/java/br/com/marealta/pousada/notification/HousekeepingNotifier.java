package br.com.marealta.pousada.notification;

import br.com.marealta.pousada.domain.ConfirmedReservationCancelled;
import br.com.marealta.pousada.domain.Receipt;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationConfirmed;

/** Avisa a governança para preparar o quarto, ou que as datas ficaram livres. */
public class HousekeepingNotifier implements Notifier {

    static final String ADDRESS = "governanca@pousadamarealta.com.br";

    private final MessageSender sender;

    public HousekeepingNotifier(MessageSender sender) {
        this.sender = sender;
    }

    @Override
    public void subscribeTo(EventBus bus) {
        bus.subscribe(ReservationConfirmed.class, e -> onConfirmed(e.reservation()));
        bus.subscribe(ConfirmedReservationCancelled.class, e -> onCancelled(e.reservation()));
    }

    private void onConfirmed(Reservation r) {
        String body = "A reserva nº " + r.id() + " do quarto " + r.room().number() + " foi confirmada.\n"
                + "Entrada: " + Receipt.formatDate(r.checkIn()) + " | Saída: " + Receipt.formatDate(r.checkOut()) + "\n"
                + "Hóspedes: " + r.guests();
        sender.send(Channel.EMAIL, ADDRESS, "Reserva confirmada - quarto " + r.room().number(), body);
    }

    private void onCancelled(Reservation r) {
        String body = "A reserva nº " + r.id() + " do quarto " + r.room().number() + " foi cancelada.\n"
                + "Datas que ficaram livres: de " + Receipt.formatDate(r.checkIn())
                + " a " + Receipt.formatDate(r.checkOut());
        sender.send(Channel.EMAIL, ADDRESS, "Reserva cancelada - quarto " + r.room().number(), body);
    }
}

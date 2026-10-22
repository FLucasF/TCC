package pousada.notification;

import pousada.app.*;
import pousada.domain.Formats;
import pousada.domain.Reservation;

public class HousekeepingNotifier implements EventSubscriber {
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
        sender.send(Message.Channel.EMAIL, ADDRESS, "Reserva confirmada - quarto " + r.room().number(),
                "A reserva nº " + r.id() + " do quarto " + r.room().number() + " foi confirmada.\n"
                        + "Entrada: " + Formats.date(r.checkIn()) + " | Saída: " + Formats.date(r.checkOut()) + "\n"
                        + "Hóspedes: " + r.guests());
    }

    private void onCancelled(Reservation r) {
        sender.send(Message.Channel.EMAIL, ADDRESS, "Reserva cancelada - quarto " + r.room().number(),
                "A reserva nº " + r.id() + " do quarto " + r.room().number() + " foi cancelada.\n"
                        + "Datas que ficaram livres: de " + Formats.date(r.checkIn())
                        + " (entrada) a " + Formats.date(r.checkOut()) + " (saída)");
    }
}

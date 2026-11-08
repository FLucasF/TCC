package br.com.marealta.pousada.notification;

import br.com.marealta.pousada.domain.Message;
import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationStatus;
import br.com.marealta.pousada.persistence.MessageRepository;

/** Avisa a governança para preparar o quarto, ou que as datas ficaram livres. */
public class HousekeepingNotifier implements ReservationListener {
    private final MessageRepository outbox;
    private final String address;

    public HousekeepingNotifier(MessageRepository outbox, String address) {
        this.outbox = outbox;
        this.address = address;
    }

    @Override
    public void onConfirmed(Reservation r) {
        outbox.send(Message.Channel.EMAIL, address, "Reserva confirmada - quarto " + r.room().number(),
                "A reserva " + r.id() + " do quarto " + r.room().number() + " foi confirmada.\n"
                        + "Entrada: " + Money.brDate(r.checkIn()) + " | Saída: " + Money.brDate(r.checkOut()) + "\n"
                        + "Hóspedes: " + r.guests());
    }

    @Override
    public void onCancelled(Reservation r, ReservationStatus previousStatus) {
        if (previousStatus != ReservationStatus.CONFIRMED) {
            return;
        }
        outbox.send(Message.Channel.EMAIL, address, "Reserva cancelada - quarto " + r.room().number(),
                "A reserva " + r.id() + " do quarto " + r.room().number() + " foi cancelada.\n"
                        + "Datas liberadas: de " + Money.brDate(r.checkIn()) + " a " + Money.brDate(r.checkOut()));
    }
}

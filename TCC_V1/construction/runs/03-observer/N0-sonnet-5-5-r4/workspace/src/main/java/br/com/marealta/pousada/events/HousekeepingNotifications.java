package br.com.marealta.pousada.events;

import br.com.marealta.pousada.domain.Message;
import br.com.marealta.pousada.domain.Reservation;
import java.util.List;

import static br.com.marealta.pousada.domain.Receipt.DATE;

/** Governança: precisa preparar o quarto quando confirma, e saber das datas livres quando cancela. */
public class HousekeepingNotifications implements NotificationRule {
    private final String address;

    public HousekeepingNotifications(String address) {
        this.address = address;
    }

    @Override
    public List<Message> messagesFor(ReservationEvent event) {
        Reservation r = event.reservation();
        int room = event.room().number();
        return switch (event) {
            case ReservationEvent.Confirmed c -> List.of(Message.email(address,
                    "Reserva confirmada - quarto " + room,
                    "A reserva nº " + r.id() + " do quarto " + room + " foi confirmada.\n"
                            + "Entrada: " + DATE.format(r.checkIn()) + "\n"
                            + "Saída: " + DATE.format(r.checkOut()) + "\n"
                            + "Hóspedes: " + r.guests()));
            case ReservationEvent.Cancelled c when c.wasPaid() -> List.of(Message.email(address,
                    "Reserva cancelada - quarto " + room,
                    "A reserva nº " + r.id() + " do quarto " + room + " foi cancelada.\n"
                            + "Datas liberadas: de " + DATE.format(r.checkIn()) + " a " + DATE.format(r.checkOut())));
            case ReservationEvent.Cancelled c -> List.of();
        };
    }
}

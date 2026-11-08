package br.com.pousadamarealta.reservas.notification;

import br.com.pousadamarealta.reservas.domain.Receipt;

import java.util.List;

/** Avisa a governança para preparar (ou liberar) o quarto. */
public class HousekeepingNotifier implements Notifier {
    private final String address;

    public HousekeepingNotifier(String address) {
        this.address = address;
    }

    @Override
    public List<Message> messagesFor(ReservationEvent event) {
        var reservation = event.reservation();
        var room = event.room();
        String dates = "Entrada: " + Receipt.formatDate(reservation.checkIn()) + "\n"
                + "Saída: " + Receipt.formatDate(reservation.checkOut());
        return switch (event) {
            case ReservationEvent.Confirmed c -> List.of(Message.email(address,
                    "Reserva confirmada - quarto " + room.number(),
                    "A reserva nº " + reservation.id() + " foi confirmada para o quarto " + room.number()
                            + " (" + room.type() + ").\n"
                            + dates + "\n"
                            + "Hóspedes: " + reservation.guests()));
            case ReservationEvent.Cancelled c when c.wasConfirmed() -> List.of(Message.email(address,
                    "Reserva cancelada - quarto " + room.number(),
                    "A reserva nº " + reservation.id() + " do quarto " + room.number() + " foi cancelada.\n"
                            + "Datas liberadas:\n"
                            + dates));
            case ReservationEvent.Cancelled c -> List.of();
        };
    }
}

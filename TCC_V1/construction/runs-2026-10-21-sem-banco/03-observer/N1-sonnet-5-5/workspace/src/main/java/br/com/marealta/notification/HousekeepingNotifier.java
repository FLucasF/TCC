package br.com.marealta.notification;

import br.com.marealta.domain.Formats;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;

/** Avisa a governança para preparar o quarto, ou que ele ficou livre. */
public class HousekeepingNotifier implements ReservationListener {
    static final String ADDRESS = "governanca@pousadamarealta.com.br";

    private final MessageSender sender;

    public HousekeepingNotifier(MessageSender sender) {
        this.sender = sender;
    }

    @Override
    public void on(ReservationEvent event) {
        switch (event) {
            case ReservationEvent.Confirmed confirmed -> {
                Reservation r = confirmed.reservation();
                sender.email(ADDRESS, "Reserva confirmada - quarto " + r.room().number(), """
                        Reserva %d confirmada para o quarto %d.

                        Entrada: %s
                        Saída: %s
                        Hóspedes: %d""".formatted(r.id(), r.room().number(),
                        Formats.date(r.checkInDate()), Formats.date(r.checkOutDate()), r.guests()));
            }
            case ReservationEvent.Cancelled cancelled
                    when cancelled.previousStatus() == ReservationStatus.CONFIRMED -> {
                Reservation r = cancelled.reservation();
                sender.email(ADDRESS, "Reserva cancelada - quarto " + r.room().number(), """
                        Reserva %d cancelada. O quarto %d ficou livre nas datas:

                        Entrada: %s
                        Saída: %s""".formatted(r.id(), r.room().number(),
                        Formats.date(r.checkInDate()), Formats.date(r.checkOutDate())));
            }
            default -> {
            }
        }
    }
}

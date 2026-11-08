package br.com.marealta;

import java.util.List;

/** Avisa a governança para preparar (ou liberar) o quarto. */
public class HousekeepingNotifier implements ReservationListener {
    private final String address;

    public HousekeepingNotifier(String address) {
        this.address = address;
    }

    @Override
    public List<Message> confirmed(Reservation r) {
        String body = "A reserva nº " + r.id() + " foi confirmada para o quarto " + r.roomNumber() + ".\n"
                + "Entrada: " + Format.date(r.checkIn()) + " | Saída: " + Format.date(r.checkOut()) + "\n"
                + "Hóspedes: " + r.guests();
        return List.of(Message.email(address, "Reserva confirmada - quarto " + r.roomNumber(), body));
    }

    @Override
    public List<Message> cancelledAfterConfirmation(Reservation r) {
        String body = "A reserva nº " + r.id() + " do quarto " + r.roomNumber() + " foi cancelada.\n"
                + "Datas que ficaram livres: de " + Format.date(r.checkIn()) + " (entrada) a "
                + Format.date(r.checkOut()) + " (saída)";
        return List.of(Message.email(address, "Reserva cancelada - quarto " + r.roomNumber(), body));
    }
}

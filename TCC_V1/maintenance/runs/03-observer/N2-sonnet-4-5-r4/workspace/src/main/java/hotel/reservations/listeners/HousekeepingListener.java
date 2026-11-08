package hotel.reservations.listeners;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.rooms.Room;

public final class HousekeepingListener implements ReservationListener {

    private final EmailSender emailSender;

    public HousekeepingListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, Room room) {
        emailSender.send(
                "governanca@pousadamarealta.com.br",
                "Reserva confirmada - quarto " + room.number(),
                "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                        + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                        + "Hóspedes: " + reservation.guests() + "\n");
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room, boolean wasPreviouslyConfirmed) {
        if (wasPreviouslyConfirmed) {
            emailSender.send(
                    "governanca@pousadamarealta.com.br",
                    "Reserva cancelada - quarto " + room.number(),
                    "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                            + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n");
        }
    }
}

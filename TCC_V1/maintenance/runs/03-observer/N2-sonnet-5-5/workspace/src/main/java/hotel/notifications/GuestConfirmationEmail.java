package hotel.notifications;

import hotel.documents.ReservationReceipt;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.rooms.Room;

/** The guest gets the receipt by e-mail when the reservation is confirmed. */
public final class GuestConfirmationEmail implements ReservationListener {

    static final String HOTEL_NAME = "Pousada Maré Alta";

    private final EmailSender emailSender;

    public GuestConfirmationEmail(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmed(Reservation reservation, Room room) {
        ReservationReceipt receipt = new ReservationReceipt(reservation, room);
        emailSender.send(
                reservation.guest().email(),
                "Reserva confirmada - " + HOTEL_NAME,
                "Olá, " + reservation.guest().name() + "!\n\n"
                        + "Sua reserva na " + HOTEL_NAME + " está confirmada. Estes são os dados dela:\n\n"
                        + receipt.text() + "\n"
                        + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                        + "Até breve!\n");
    }
}

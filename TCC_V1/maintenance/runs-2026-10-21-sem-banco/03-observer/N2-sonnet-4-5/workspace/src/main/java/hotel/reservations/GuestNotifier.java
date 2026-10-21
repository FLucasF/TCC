package hotel.reservations;

import hotel.documents.ReservationReceipt;
import hotel.messages.EmailSender;
import hotel.rooms.Room;

/** Sends confirmation e-mails to guests. */
public final class GuestNotifier implements ReservationListener {

    private static final String HOTEL_NAME = "Pousada Maré Alta";

    private final EmailSender emailSender;

    public GuestNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, Room room) {
        ReservationReceipt receipt = new ReservationReceipt(reservation, room);
        emailSender.send(
                reservation.guest().email(),
                "Reserva confirmada - " + HOTEL_NAME,
                confirmationEmailBody(reservation, receipt));
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room) {
        // Guest does not receive cancellation e-mails
    }

    private static String confirmationEmailBody(Reservation reservation, ReservationReceipt receipt) {
        return "Olá, " + reservation.guest().name() + "!\n\n"
                + "Sua reserva na " + HOTEL_NAME + " está confirmada. Estes são os dados dela:\n\n"
                + receipt.text() + "\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!\n";
    }
}

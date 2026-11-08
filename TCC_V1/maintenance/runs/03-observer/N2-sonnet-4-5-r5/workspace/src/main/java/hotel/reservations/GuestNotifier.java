package hotel.reservations;

import hotel.documents.ReservationReceipt;
import hotel.messages.EmailSender;
import hotel.rooms.Room;

/** Sends the confirmation receipt to the guest by e-mail. */
public final class GuestNotifier implements ReservationListener {

    private final EmailSender emailSender;

    public GuestNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmed(Reservation reservation, Room room) {
        ReservationReceipt receipt = new ReservationReceipt(reservation, room);
        emailSender.send(
                reservation.guest().email(),
                "Reserva confirmada - " + ReservationService.HOTEL_NAME,
                confirmationEmailBody(reservation, receipt));
    }

    @Override
    public void onCancelled(Reservation reservation, Room room) {
        // the guest does not get an e-mail when cancelling
    }

    private static String confirmationEmailBody(Reservation reservation, ReservationReceipt receipt) {
        return "Olá, " + reservation.guest().name() + "!\n\n"
                + "Sua reserva na " + ReservationService.HOTEL_NAME + " está confirmada. Estes são os dados dela:\n\n"
                + receipt.text() + "\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!\n";
    }
}

package hotel.reservations;

import hotel.documents.ReservationReceipt;
import hotel.messages.EmailSender;

final class GuestConfirmationListener implements ReservationListener {

    private static final String HOTEL_NAME = "Pousada Maré Alta";
    private final EmailSender emailSender;

    GuestConfirmationListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(ReservationConfirmedEvent event) {
        Reservation reservation = event.reservation();
        ReservationReceipt receipt = new ReservationReceipt(reservation, event.room());
        String body = "Olá, " + reservation.guest().name() + "!\n\n"
                + "Sua reserva na " + HOTEL_NAME + " está confirmada. Estes são os dados dela:\n\n"
                + receipt.text() + "\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!\n";
        emailSender.send(
                reservation.guest().email(),
                "Reserva confirmada - " + HOTEL_NAME,
                body);
    }

    @Override
    public void onReservationCancelled(ReservationCancelledEvent event) {
    }
}

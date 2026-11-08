package hotel.notifications;

import hotel.documents.ReservationReceipt;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.rooms.Rooms;

/** Sends the guest the receipt when the reservation is confirmed. */
public final class GuestReceiptEmail implements ReservationListener {

    static final String HOTEL_NAME = "Pousada Maré Alta";

    private final Rooms rooms;
    private final EmailSender emailSender;

    public GuestReceiptEmail(Rooms rooms, EmailSender emailSender) {
        this.rooms = rooms;
        this.emailSender = emailSender;
    }

    @Override
    public void confirmed(Reservation reservation) {
        ReservationReceipt receipt = new ReservationReceipt(
                reservation, rooms.find(reservation.roomNumber()).orElseThrow());
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

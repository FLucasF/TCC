package hotel.reservations;

import hotel.documents.ReservationReceipt;
import hotel.messages.EmailSender;
import hotel.rooms.Room;
import hotel.rooms.Rooms;

public final class GuestNotifier implements ReservationEventListener {

    private static final String HOTEL_NAME = "Pousada Maré Alta";

    private final EmailSender emailSender;
    private final Rooms rooms;

    public GuestNotifier(EmailSender emailSender, Rooms rooms) {
        this.emailSender = emailSender;
        this.rooms = rooms;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        ReservationReceipt receipt = new ReservationReceipt(reservation, roomOf(reservation));
        emailSender.send(
                reservation.guest().email(),
                "Reserva confirmada - " + HOTEL_NAME,
                confirmationEmailBody(reservation, receipt));
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
    }

    private String confirmationEmailBody(Reservation reservation, ReservationReceipt receipt) {
        return "Olá, " + reservation.guest().name() + "!\n\n"
                + "Sua reserva na " + HOTEL_NAME + " está confirmada. Estes são os dados dela:\n\n"
                + receipt.text() + "\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!\n";
    }

    private Room roomOf(Reservation reservation) {
        return rooms.find(reservation.roomNumber()).orElseThrow();
    }
}

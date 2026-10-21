package hotel.reservations;

import hotel.documents.ReservationReceipt;
import hotel.messages.EmailSender;
import hotel.rooms.Room;
import hotel.rooms.Rooms;

public final class GuestNotifier implements ReservationNotifier {

    private static final String HOTEL_NAME = "Pousada Maré Alta";

    private final Rooms rooms;
    private final EmailSender emailSender;

    public GuestNotifier(Rooms rooms, EmailSender emailSender) {
        this.rooms = rooms;
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmation(Reservation reservation) {
        Room room = roomOf(reservation);
        ReservationReceipt receipt = new ReservationReceipt(reservation, room);
        emailSender.send(
                reservation.guest().email(),
                "Reserva confirmada - " + HOTEL_NAME,
                confirmationEmailBody(reservation, receipt));
    }

    @Override
    public void onCancellation(Reservation reservation) {
    }

    private Room roomOf(Reservation reservation) {
        return rooms.find(reservation.roomNumber()).orElseThrow();
    }

    private static String confirmationEmailBody(Reservation reservation, ReservationReceipt receipt) {
        return "Olá, " + reservation.guest().name() + "!\n\n"
                + "Sua reserva na " + HOTEL_NAME + " está confirmada. Estes são os dados dela:\n\n"
                + receipt.text() + "\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!\n";
    }
}

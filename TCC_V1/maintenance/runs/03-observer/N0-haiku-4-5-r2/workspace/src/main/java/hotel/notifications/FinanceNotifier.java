package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

public final class FinanceNotifier implements Notifier {

    private final String email;
    private final EmailSender emailSender;

    public FinanceNotifier(String email, EmailSender emailSender) {
        this.email = email;
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, Room room) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Valor: " + TextFormat.money(reservation.amountPaid()) + "\n";
        emailSender.send(email, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room) {
        if (reservation.refund() != null && reservation.refund().signum() > 0) {
            String subject = "Reembolso - reserva " + reservation.id();
            String body = "Valor: " + TextFormat.money(reservation.refund()) + "\n";
            emailSender.send(email, subject, body);
        }
    }
}

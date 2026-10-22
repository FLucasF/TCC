package hotel.reservations.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStatus;
import hotel.rooms.Room;

class FinanceNotifier implements ReservationEventNotifier {
    private final EmailSender emailSender;
    private final String financeEmail;

    FinanceNotifier(EmailSender emailSender, String financeEmail) {
        this.emailSender = emailSender;
        this.financeEmail = financeEmail;
    }

    @Override
    public void notifyConfirmation(Reservation reservation, Room room) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Valor: " + TextFormat.money(reservation.amountPaid());
        emailSender.send(financeEmail, subject, body);
    }

    @Override
    public void notifyCancellation(Reservation reservation, Room room) {
        if (reservation.status() != ReservationStatus.CANCELLED || reservation.refund() == null
                || reservation.refund().signum() == 0) {
            return;
        }
        String subject = "Reembolso - reserva " + reservation.id();
        String body = "Valor: " + TextFormat.money(reservation.refund());
        emailSender.send(financeEmail, subject, body);
    }
}

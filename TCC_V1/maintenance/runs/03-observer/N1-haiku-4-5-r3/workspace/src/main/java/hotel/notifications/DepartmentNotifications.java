package hotel.notifications;

import hotel.documents.ReservationReceipt;
import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;

public final class DepartmentNotifications {

    private final EmailSender emailSender;

    public DepartmentNotifications(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void notifyConfirmation(Reservation reservation, Room room) {
        notifyGovernanceConfirmation(reservation, room);
        notifyFinanceConfirmation(reservation);
    }

    public void notifyCancellation(Reservation reservation, Room room) {
        notifyGovernanceCancellation(reservation, room);
        notifyFinanceCancellation(reservation);
    }

    private void notifyGovernanceConfirmation(Reservation reservation, Room room) {
        String subject = "Reserva confirmada - quarto " + room.number();
        String body = governanceConfirmationBody(reservation);
        emailSender.send("governanca@pousadamarealta.com.br", subject, body);
    }

    private void notifyFinanceConfirmation(Reservation reservation) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = financeConfirmationBody(reservation);
        emailSender.send("financeiro@pousadamarealta.com.br", subject, body);
    }

    private void notifyGovernanceCancellation(Reservation reservation, Room room) {
        String subject = "Reserva cancelada - quarto " + room.number();
        String body = governanceCancellationBody(reservation);
        emailSender.send("governanca@pousadamarealta.com.br", subject, body);
    }

    private void notifyFinanceCancellation(Reservation reservation) {
        BigDecimal refund = reservation.refund();
        if (refund == null || refund.signum() == 0) {
            return;
        }
        String subject = "Reembolso - reserva " + reservation.id();
        String body = financeCancellationBody(reservation);
        emailSender.send("financeiro@pousadamarealta.com.br", subject, body);
    }

    private static String governanceConfirmationBody(Reservation reservation) {
        return "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests();
    }

    private static String financeConfirmationBody(Reservation reservation) {
        return "Valor: " + TextFormat.money(reservation.amountPaid());
    }

    private static String governanceCancellationBody(Reservation reservation) {
        return "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut());
    }

    private static String financeCancellationBody(Reservation reservation) {
        return "Valor: " + TextFormat.money(reservation.refund());
    }
}

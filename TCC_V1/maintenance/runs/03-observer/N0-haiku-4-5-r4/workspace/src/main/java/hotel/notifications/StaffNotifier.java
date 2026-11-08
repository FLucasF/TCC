package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Notifies staff members about reservation events. */
public final class StaffNotifier {

    private final EmailSender emailSender;

    public StaffNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void notifyReservationConfirmed(Reservation reservation) {
        notifyGovernance(reservation);
        notifyFinance(reservation);
    }

    public void notifyReservationCancelled(Reservation reservation) {
        notifyGovernanceOfCancellation(reservation);
        if (reservation.refund().signum() > 0) {
            notifyFinanceOfRefund(reservation);
        }
    }

    private void notifyGovernance(Reservation reservation) {
        String subject = "Reserva confirmada - quarto " + reservation.roomNumber();
        String body = "Reserva confirmada:\n\n"
                + "Quarto: " + reservation.roomNumber() + "\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests() + "\n";
        emailSender.send("governanca@pousadamarealta.com.br", subject, body);
    }

    private void notifyFinance(Reservation reservation) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Pagamento recebido:\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor: " + TextFormat.money(reservation.amountPaid()) + "\n";
        emailSender.send("financeiro@pousadamarealta.com.br", subject, body);
    }

    private void notifyGovernanceOfCancellation(Reservation reservation) {
        String subject = "Reserva cancelada - quarto " + reservation.roomNumber();
        String body = "Reserva cancelada:\n\n"
                + "Quarto: " + reservation.roomNumber() + "\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n";
        emailSender.send("governanca@pousadamarealta.com.br", subject, body);
    }

    private void notifyFinanceOfRefund(Reservation reservation) {
        String subject = "Reembolso - reserva " + reservation.id();
        String body = "Reembolso a processar:\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor: " + TextFormat.money(reservation.refund()) + "\n";
        emailSender.send("financeiro@pousadamarealta.com.br", subject, body);
    }
}

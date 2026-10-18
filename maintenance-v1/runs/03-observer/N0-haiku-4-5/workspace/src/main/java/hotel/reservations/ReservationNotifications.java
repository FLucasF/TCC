package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.rooms.Room;
import java.math.BigDecimal;

/** Manages notifications to staff when reservations are confirmed or cancelled. */
public final class ReservationNotifications {

    private static final String HOTEL_NAME = "Pousada Maré Alta";
    private static final String GOVERNANCE_EMAIL = "governanca@pousadamarealta.com.br";
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public ReservationNotifications(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    /** Notify governance and finance when a reservation is confirmed. */
    public void notifyConfirmation(Reservation reservation, Room room) {
        notifyGovernanceConfirmation(reservation, room);
        notifyFinanceConfirmation(reservation);
    }

    /** Notify governance and finance when a confirmed reservation is cancelled. */
    public void notifyCancellation(Reservation reservation, Room room) {
        notifyGovernanceCancellation(reservation, room);
        if (reservation.refund().signum() > 0) {
            notifyFinanceCancellation(reservation);
        }
    }

    private void notifyGovernanceConfirmation(Reservation reservation, Room room) {
        String subject = "Reserva confirmada - quarto " + room.number();
        String body = "Quarto " + room.number() + "\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests();
        emailSender.send(GOVERNANCE_EMAIL, subject, body);
    }

    private void notifyFinanceConfirmation(Reservation reservation) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Valor: " + TextFormat.money(reservation.amountPaid());
        emailSender.send(FINANCE_EMAIL, subject, body);
    }

    private void notifyGovernanceCancellation(Reservation reservation, Room room) {
        String subject = "Reserva cancelada - quarto " + room.number();
        String body = "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut());
        emailSender.send(GOVERNANCE_EMAIL, subject, body);
    }

    private void notifyFinanceCancellation(Reservation reservation) {
        String subject = "Reembolso - reserva " + reservation.id();
        String body = "Valor: " + TextFormat.money(reservation.refund());
        emailSender.send(FINANCE_EMAIL, subject, body);
    }
}

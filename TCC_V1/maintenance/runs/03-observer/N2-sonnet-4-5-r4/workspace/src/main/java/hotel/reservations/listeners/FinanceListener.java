package hotel.reservations.listeners;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.rooms.Room;
import java.math.BigDecimal;

public final class FinanceListener implements ReservationListener {

    private final EmailSender emailSender;

    public FinanceListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, Room room) {
        emailSender.send(
                "financeiro@pousadamarealta.com.br",
                "Pagamento recebido - reserva " + reservation.id(),
                "Valor: " + TextFormat.money(reservation.amountPaid()) + "\n");
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room, boolean wasPreviouslyConfirmed) {
        if (wasPreviouslyConfirmed && reservation.refund().signum() > 0) {
            emailSender.send(
                    "financeiro@pousadamarealta.com.br",
                    "Reembolso - reserva " + reservation.id(),
                    "Valor: " + TextFormat.money(reservation.refund()) + "\n");
        }
    }
}

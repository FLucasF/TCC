package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.rooms.Room;
import java.math.BigDecimal;

/** Tells housekeeping and finance when a reservation is confirmed or a confirmed one is cancelled. */
public final class StaffNotifications implements ReservationListener {

    public static final String HOUSEKEEPING = "governanca@pousadamarealta.com.br";
    public static final String FINANCE = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public StaffNotifications(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void confirmed(Reservation reservation, Room room) {
        emailSender.send(
                HOUSEKEEPING,
                "Reserva confirmada - quarto " + room.number(),
                "Reserva " + reservation.id() + " confirmada para o quarto " + room.number() + ".\n"
                        + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                        + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                        + "Hóspedes: " + reservation.guests() + "\n");
        emailSender.send(
                FINANCE,
                "Pagamento recebido - reserva " + reservation.id(),
                "Pagamento recebido da reserva " + reservation.id() + ".\n"
                        + "Valor pago: " + TextFormat.money(reservation.amountPaid()) + "\n");
    }

    @Override
    public void cancelledAfterConfirmation(Reservation reservation, Room room, BigDecimal refund) {
        emailSender.send(
                HOUSEKEEPING,
                "Reserva cancelada - quarto " + room.number(),
                "A reserva " + reservation.id() + " do quarto " + room.number() + " foi cancelada.\n"
                        + "Datas liberadas: de " + TextFormat.date(reservation.checkIn())
                        + " a " + TextFormat.date(reservation.checkOut()) + "\n");
        if (refund.signum() > 0) {
            emailSender.send(
                    FINANCE,
                    "Reembolso - reserva " + reservation.id(),
                    "Reembolso da reserva " + reservation.id() + " cancelada.\n"
                            + "Valor a devolver: " + TextFormat.money(refund) + "\n");
        }
    }
}

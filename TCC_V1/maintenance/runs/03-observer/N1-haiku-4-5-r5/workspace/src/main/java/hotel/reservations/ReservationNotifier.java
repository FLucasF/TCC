package hotel.reservations;

import hotel.documents.ReservationReceipt;
import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.rooms.Room;
import java.math.BigDecimal;

/** Sends notifications to staff and guests when reservations change. */
public final class ReservationNotifier {

    private final EmailSender emailSender;

    public ReservationNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    void notifyConfirmation(Reservation reservation, Room room) {
        ReservationReceipt receipt = new ReservationReceipt(reservation, room);

        emailSender.send(
                reservation.guest().email(),
                "Reserva confirmada - Pousada Maré Alta",
                guestConfirmationBody(reservation, receipt));

        emailSender.send(
                "governanca@pousadamarealta.com.br",
                "Reserva confirmada - quarto " + room.number(),
                housekeepingConfirmationBody(reservation, room));

        emailSender.send(
                "financeiro@pousadamarealta.com.br",
                "Pagamento recebido - reserva " + reservation.id(),
                financeConfirmationBody(reservation));
    }

    void notifyCancellation(Reservation reservation, Room room) {
        emailSender.send(
                "governanca@pousadamarealta.com.br",
                "Reserva cancelada - quarto " + room.number(),
                housekeepingCancellationBody(reservation));

        if (reservation.refund().signum() > 0) {
            emailSender.send(
                    "financeiro@pousadamarealta.com.br",
                    "Reembolso - reserva " + reservation.id(),
                    financeCancellationBody(reservation));
        }
    }

    private static String guestConfirmationBody(Reservation reservation, ReservationReceipt receipt) {
        return "Olá, " + reservation.guest().name() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt.text() + "\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!\n";
    }

    private static String housekeepingConfirmationBody(Reservation reservation, Room room) {
        return "Quarto " + room.number() + " reservado:\n\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests() + "\n";
    }

    private static String financeConfirmationBody(Reservation reservation) {
        return "Pagamento recebido:\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor: " + TextFormat.money(reservation.amountPaid()) + "\n";
    }

    private static String housekeepingCancellationBody(Reservation reservation) {
        return "Quarto " + reservation.roomNumber() + " liberado:\n\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n";
    }

    private static String financeCancellationBody(Reservation reservation) {
        return "Reembolso:\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor: " + TextFormat.money(reservation.refund()) + "\n";
    }
}

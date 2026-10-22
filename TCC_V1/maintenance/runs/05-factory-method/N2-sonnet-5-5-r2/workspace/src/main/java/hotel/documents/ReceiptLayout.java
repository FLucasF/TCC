package hotel.documents;

import hotel.reservations.Price;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** The receipt for a guest with CPF, in Portuguese. */
public class ReceiptLayout extends DocumentLayout {

    @Override
    protected String title() {
        return "Comprovante de reserva";
    }

    @Override
    protected String headerLine(long reservationId) {
        return title() + " nº " + reservationId + "\n";
    }

    @Override
    protected String identityLines(Reservation reservation) {
        return "Hóspede: " + reservation.guest().name()
                + " (CPF " + reservation.guest().identification().fields().cpf() + ")\n";
    }

    @Override
    protected String roomLine(Room room) {
        return "Quarto: " + room.number() + " (" + room.type() + ")\n";
    }

    @Override
    protected String stayLine(Reservation reservation) {
        int nights = reservation.price().nights();
        return "Entrada: " + TextFormat.date(reservation.checkIn())
                + " | Saída: " + TextFormat.date(reservation.checkOut())
                + " | " + nights + (nights == 1 ? " diária" : " diárias") + "\n";
    }

    @Override
    protected String priceLines(Price price) {
        String lines = "Diárias: " + TextFormat.money(price.subtotal()) + "\n";
        if (price.discount().signum() > 0) {
            lines += "Desconto: " + TextFormat.money(price.discount()) + "\n";
        }
        return lines + "Total: " + TextFormat.money(price.total()) + "\n";
    }
}

package hotel.documents;

import hotel.reservations.Price;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** The receipt in Portuguese, for the guest identified by CPF. */
public class ReceiptLayout extends DocumentLayout {

    @Override
    public String title() {
        return "Comprovante de reserva";
    }

    @Override
    protected String header(Reservation reservation) {
        return title() + " nº " + reservation.id();
    }

    @Override
    protected String guestLines(Reservation reservation) {
        return "Hóspede: " + reservation.guest().name() + " (CPF " + reservation.guest().identification().cpf() + ")";
    }

    @Override
    protected String roomLine(Room room) {
        return "Quarto: " + room.number() + " (" + room.type() + ")";
    }

    @Override
    protected String stayLine(Reservation reservation) {
        int nights = reservation.price().nights();
        return "Entrada: " + TextFormat.date(reservation.checkIn())
                + " | Saída: " + TextFormat.date(reservation.checkOut())
                + " | " + nights + (nights == 1 ? " diária" : " diárias");
    }

    @Override
    protected String chargesLine(Price price) {
        return "Diárias: " + TextFormat.money(price.subtotal());
    }

    @Override
    protected String discountLine(Price price) {
        return "Desconto: " + TextFormat.money(price.discount());
    }

    @Override
    protected String totalLine(Price price) {
        return "Total: " + TextFormat.money(price.total());
    }
}

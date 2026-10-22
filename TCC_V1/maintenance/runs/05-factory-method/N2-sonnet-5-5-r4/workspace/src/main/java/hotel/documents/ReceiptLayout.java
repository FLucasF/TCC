package hotel.documents;

import hotel.reservations.Guest;
import java.math.BigDecimal;
import java.time.LocalDate;

/** The receipt of a guest identified by CPF, in Portuguese. */
class ReceiptLayout extends DocumentLayout {

    @Override
    String title() {
        return "Comprovante de reserva";
    }

    @Override
    String numberMark() {
        return "nº";
    }

    @Override
    String party(Guest guest) {
        return "Hóspede: " + guest.name() + " (CPF " + guest.identification().cpf() + ")\n";
    }

    @Override
    String roomLabel() {
        return "Quarto";
    }

    @Override
    String checkInLabel() {
        return "Entrada";
    }

    @Override
    String checkOutLabel() {
        return "Saída";
    }

    @Override
    String nights(int count) {
        return count + (count == 1 ? " diária" : " diárias");
    }

    @Override
    String subtotalLabel() {
        return "Diárias";
    }

    @Override
    String discountLabel() {
        return "Desconto";
    }

    @Override
    String totalLabel() {
        return "Total";
    }

    @Override
    String money(BigDecimal amount) {
        return TextFormat.money(amount);
    }

    @Override
    String date(LocalDate date) {
        return TextFormat.date(date);
    }
}

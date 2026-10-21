package hotel.documents;

import hotel.reservations.Reservation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** The Portuguese receipt for a guest with a CPF. */
class ReceiptLayout extends DocumentLayout {

    @Override
    String title() {
        return "Comprovante de reserva";
    }

    @Override
    String numberSign() {
        return "nº";
    }

    @Override
    List<String> identityLines(Reservation reservation) {
        return List.of("Hóspede: " + reservation.guest().name()
                + " (CPF " + reservation.guest().identification().cpf() + ")");
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

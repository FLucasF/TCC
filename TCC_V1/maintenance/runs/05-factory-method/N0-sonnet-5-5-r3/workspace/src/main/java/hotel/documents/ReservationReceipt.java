package hotel.documents;

import com.fasterxml.jackson.annotation.JsonProperty;
import hotel.reservations.PriceCalculator;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The document the guest gets when the reservation is confirmed, at check-out, and on request: the receipt
 * for a guest with a CPF, the invoice for a company's guest, the booking receipt (in English) for a foreigner.
 */
public record ReservationReceipt(
        String title,
        long reservationId,
        String guestName,
        String guestCpf,
        String companyCnpj,
        String companyName,
        String guestPassport,
        String guestCountry,
        int roomNumber,
        RoomType roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total) {

    private static final BigDecimal WITHHELD_TAX_RATE = new BigDecimal("0.05");

    public ReservationReceipt(Reservation reservation, Room room) {
        this(
                titleOf(reservation),
                reservation.id(),
                reservation.guest().name(),
                reservation.guest().cpf(),
                reservation.guest().companyCnpj(),
                reservation.guest().companyName(),
                reservation.guest().passport(),
                reservation.guest().country(),
                room.number(),
                room.type(),
                reservation.checkIn(),
                reservation.checkOut(),
                reservation.price().nights(),
                reservation.price().subtotal(),
                reservation.price().discount(),
                reservation.price().total());
    }

    private static String titleOf(Reservation reservation) {
        if (reservation.guest().isCompany()) {
            return "Fatura de hospedagem";
        }
        return reservation.guest().isForeigner() ? "Booking receipt" : "Comprovante de reserva";
    }

    /** The document as it is written in the confirmation e-mail. */
    @JsonProperty("text")
    public String text() {
        if (companyCnpj != null) {
            return invoiceText();
        }
        return guestPassport != null ? bookingReceiptText() : receiptText();
    }

    private String receiptText() {
        StringBuilder text = new StringBuilder();
        text.append(title).append(" nº ").append(reservationId).append('\n');
        text.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        appendStayInPortuguese(text);
        return text.toString();
    }

    private String invoiceText() {
        StringBuilder text = new StringBuilder();
        text.append(title).append(" nº ").append(reservationId).append('\n');
        text.append("Empresa: ").append(companyName).append(" (CNPJ ").append(companyCnpj).append(")\n");
        text.append("Hóspede: ").append(guestName).append('\n');
        appendStayInPortuguese(text);
        BigDecimal withheldTax = PriceCalculator.cents(total.multiply(WITHHELD_TAX_RATE));
        text.append("ISS retido (5%): ").append(TextFormat.money(withheldTax)).append('\n');
        return text.toString();
    }

    private void appendStayInPortuguese(StringBuilder text) {
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(checkIn))
                .append(" | Saída: ").append(TextFormat.date(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(discount)).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(total)).append('\n');
    }

    private String bookingReceiptText() {
        StringBuilder text = new StringBuilder();
        text.append(title).append(" no. ").append(reservationId).append('\n');
        text.append("Guest: ").append(guestName).append(" (passport ").append(guestPassport)
                .append(", ").append(guestCountry).append(")\n");
        text.append("Room: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Check-in: ").append(TextFormat.dateInEnglish(checkIn))
                .append(" | Check-out: ").append(TextFormat.dateInEnglish(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(TextFormat.moneyInEnglish(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Discount: ").append(TextFormat.moneyInEnglish(discount)).append('\n');
        }
        text.append("Total: ").append(TextFormat.moneyInEnglish(total)).append('\n');
        return text.toString();
    }
}

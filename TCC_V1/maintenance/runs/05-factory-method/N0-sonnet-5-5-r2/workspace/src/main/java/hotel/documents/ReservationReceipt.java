package hotel.documents;

import com.fasterxml.jackson.annotation.JsonProperty;
import hotel.reservations.PriceCalculator;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The document the guest gets when the reservation is confirmed, at check-out, and on request. Which one
 * depends on how the guest is identified: a receipt (CPF), an invoice (company) or a booking receipt
 * (foreigner, in English).
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

    /** The ISS withheld on an invoice: 5% of the total. */
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    public ReservationReceipt(Reservation reservation, Room room) {
        this(
                titleFor(reservation),
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

    private static String titleFor(Reservation reservation) {
        if (reservation.guest().companyCnpj() != null) {
            return "Fatura de hospedagem";
        }
        if (reservation.guest().passport() != null) {
            return "Booking receipt";
        }
        return "Comprovante de reserva";
    }

    /** The document as it goes in the e-mail. */
    @JsonProperty("text")
    public String text() {
        if (companyCnpj != null) {
            return invoiceText();
        }
        if (guestPassport != null) {
            return bookingReceiptText();
        }
        return receiptText();
    }

    private String receiptText() {
        StringBuilder text = new StringBuilder();
        text.append(title).append(" nº ").append(reservationId).append('\n');
        text.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        appendPortugueseBody(text);
        return text.toString();
    }

    private String invoiceText() {
        StringBuilder text = new StringBuilder();
        text.append(title).append(" nº ").append(reservationId).append('\n');
        text.append("Empresa: ").append(companyName).append(" (CNPJ ").append(companyCnpj).append(")\n");
        text.append("Hóspede: ").append(guestName).append('\n');
        appendPortugueseBody(text);
        BigDecimal withheld = PriceCalculator.cents(total.multiply(ISS_RATE));
        text.append("ISS retido (5%): ").append(TextFormat.money(withheld)).append('\n');
        return text.toString();
    }

    private void appendPortugueseBody(StringBuilder text) {
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
        text.append("Guest: ").append(guestName)
                .append(" (passport ").append(guestPassport).append(", ").append(guestCountry).append(")\n");
        text.append("Room: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Check-in: ").append(TextFormat.englishDate(checkIn))
                .append(" | Check-out: ").append(TextFormat.englishDate(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(TextFormat.englishMoney(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Discount: ").append(TextFormat.englishMoney(discount)).append('\n');
        }
        text.append("Total: ").append(TextFormat.englishMoney(total)).append('\n');
        return text.toString();
    }
}

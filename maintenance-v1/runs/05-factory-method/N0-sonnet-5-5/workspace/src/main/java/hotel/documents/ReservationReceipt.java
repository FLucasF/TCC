package hotel.documents;

import com.fasterxml.jackson.annotation.JsonProperty;
import hotel.reservations.GuestIdentity;
import hotel.reservations.PriceCalculator;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The document the guest gets when the reservation is confirmed, at check-out, and on request: a receipt
 * for a guest with CPF, an invoice for a company, a booking receipt (in English) for a foreigner. The
 * identification fields that do not apply to the guest are null.
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

    private static final BigDecimal WITHHELD_ISS_RATE = new BigDecimal("0.05");

    public ReservationReceipt(Reservation reservation, Room room) {
        this(
                titleFor(reservation.guest().identity()),
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

    private static String titleFor(GuestIdentity identity) {
        return switch (identity) {
            case GuestIdentity.Cpf cpf -> "Comprovante de reserva";
            case GuestIdentity.Company company -> "Fatura de hospedagem";
            case GuestIdentity.Passport passport -> "Booking receipt";
        };
    }

    /** The text of the document, as it goes in the e-mail. */
    @JsonProperty("text")
    public String text() {
        if (guestPassport != null) {
            return englishText();
        }
        return portugueseText();
    }

    private String portugueseText() {
        StringBuilder text = new StringBuilder();
        text.append(title).append(" nº ").append(reservationId).append('\n');
        if (companyCnpj != null) {
            text.append("Empresa: ").append(companyName).append(" (CNPJ ").append(companyCnpj).append(")\n");
            text.append("Hóspede: ").append(guestName).append('\n');
        } else {
            text.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        }
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(checkIn))
                .append(" | Saída: ").append(TextFormat.date(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(discount)).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(total)).append('\n');
        if (companyCnpj != null) {
            BigDecimal withheldIss = PriceCalculator.cents(total.multiply(WITHHELD_ISS_RATE));
            text.append("ISS retido (5%): ").append(TextFormat.money(withheldIss)).append('\n');
        }
        return text.toString();
    }

    private String englishText() {
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

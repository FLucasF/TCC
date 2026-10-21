package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.PriceCalculator;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The document the guest gets when the reservation is confirmed, at check-out, and on request: a receipt
 * for a CPF guest, an invoice for a company, a booking receipt (in English) for a foreign guest. The
 * identification fields that do not apply are null.
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
        BigDecimal total,
        String text) {

    private static final BigDecimal WITHHELD_ISS_RATE = new BigDecimal("0.05");

    public static ReservationReceipt of(Reservation reservation, Room room) {
        Identification id = reservation.guest().identification();
        String cpf = id instanceof Identification.Cpf c ? c.number() : null;
        String cnpj = id instanceof Identification.Company c ? c.cnpj() : null;
        String companyName = id instanceof Identification.Company c ? c.name() : null;
        String passport = id instanceof Identification.Foreign f ? f.passport() : null;
        String country = id instanceof Identification.Foreign f ? f.country() : null;
        String title = switch (id) {
            case Identification.Cpf c -> "Comprovante de reserva";
            case Identification.Company c -> "Fatura de hospedagem";
            case Identification.Foreign f -> "Booking receipt";
        };
        ReservationReceipt withoutText = new ReservationReceipt(
                title,
                reservation.id(),
                reservation.guest().name(),
                cpf,
                cnpj,
                companyName,
                passport,
                country,
                room.number(),
                room.type(),
                reservation.checkIn(),
                reservation.checkOut(),
                reservation.price().nights(),
                reservation.price().subtotal(),
                reservation.price().discount(),
                reservation.price().total(),
                null);
        return withoutText.withText(switch (id) {
            case Identification.Cpf c -> withoutText.portugueseText(
                    "Hóspede: " + withoutText.guestName + " (CPF " + cpf + ")\n", false);
            case Identification.Company c -> withoutText.portugueseText(
                    "Empresa: " + companyName + " (CNPJ " + cnpj + ")\nHóspede: " + withoutText.guestName + "\n",
                    true);
            case Identification.Foreign f -> withoutText.englishText();
        });
    }

    private ReservationReceipt withText(String newText) {
        return new ReservationReceipt(title, reservationId, guestName, guestCpf, companyCnpj, companyName,
                guestPassport, guestCountry, roomNumber, roomType, checkIn, checkOut, nights, subtotal,
                discount, total, newText);
    }

    private String portugueseText(String whoLines, boolean withheldIss) {
        StringBuilder text = new StringBuilder();
        text.append(title).append(" nº ").append(reservationId).append('\n');
        text.append(whoLines);
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(checkIn))
                .append(" | Saída: ").append(TextFormat.date(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(discount)).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(total)).append('\n');
        if (withheldIss) {
            BigDecimal iss = PriceCalculator.cents(total.multiply(WITHHELD_ISS_RATE));
            text.append("ISS retido (5%): ").append(TextFormat.money(iss)).append('\n');
        }
        return text.toString();
    }

    private String englishText() {
        StringBuilder text = new StringBuilder();
        text.append(title).append(" no. ").append(reservationId).append('\n');
        text.append("Guest: ").append(guestName)
                .append(" (passport ").append(guestPassport).append(", ").append(guestCountry).append(")\n");
        text.append("Room: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Check-in: ").append(checkIn)
                .append(" | Check-out: ").append(checkOut)
                .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(TextFormat.englishMoney(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Discount: ").append(TextFormat.englishMoney(discount)).append('\n');
        }
        text.append("Total: ").append(TextFormat.englishMoney(total)).append('\n');
        return text.toString();
    }
}

package hotel.documents;

import hotel.reservations.Guest;
import hotel.reservations.Identification;
import hotel.reservations.PriceCalculator;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The document the guest gets when the reservation is confirmed, at check-out, and on request. Which one it
 * is depends on how the guest is identified: the receipt (CPF), the lodging invoice (company) or the booking
 * receipt (passport). The fields that do not apply to the kind of document are null.
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

    public ReservationReceipt(Reservation reservation, Room room) {
        this(reservation, room, reservation.guest());
    }

    private ReservationReceipt(Reservation reservation, Room room, Guest guest) {
        this(
                titleFor(guest.identification()),
                reservation.id(),
                guest.name(),
                guest.cpf(),
                guest.companyCnpj(),
                guest.companyName(),
                guest.passport(),
                guest.country(),
                room.number(),
                room.type(),
                reservation.checkIn(),
                reservation.checkOut(),
                reservation.price().nights(),
                reservation.price().subtotal(),
                reservation.price().discount(),
                reservation.price().total(),
                null);
    }

    /** Fills in the text, which is derived from the other fields. */
    public ReservationReceipt {
        if (text == null) {
            text = render(title, reservationId, guestName, guestCpf, companyCnpj, companyName, guestPassport,
                    guestCountry, roomNumber, roomType, checkIn, checkOut, nights, subtotal, discount, total);
        }
    }

    private static String titleFor(Identification identification) {
        return switch (identification) {
            case Identification.Cpf cpf -> "Comprovante de reserva";
            case Identification.Company company -> "Fatura de hospedagem";
            case Identification.Passport passport -> "Booking receipt";
        };
    }

    private static String render(
            String title, long reservationId, String guestName, String cpf, String cnpj, String companyName,
            String passport, String country, int roomNumber, RoomType roomType, LocalDate checkIn,
            LocalDate checkOut, int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        StringBuilder text = new StringBuilder();
        if (passport != null) {
            text.append(title).append(" no. ").append(reservationId).append('\n');
            text.append("Guest: ").append(guestName)
                    .append(" (passport ").append(passport).append(", ").append(country).append(")\n");
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
        text.append(title).append(" nº ").append(reservationId).append('\n');
        if (cnpj != null) {
            text.append("Empresa: ").append(companyName).append(" (CNPJ ").append(cnpj).append(")\n");
            text.append("Hóspede: ").append(guestName).append('\n');
        } else {
            text.append("Hóspede: ").append(guestName).append(" (CPF ").append(cpf).append(")\n");
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
        if (cnpj != null) {
            BigDecimal iss = PriceCalculator.cents(total.multiply(WITHHELD_ISS_RATE));
            text.append("ISS retido (5%): ").append(TextFormat.money(iss)).append('\n');
        }
        return text.toString();
    }
}

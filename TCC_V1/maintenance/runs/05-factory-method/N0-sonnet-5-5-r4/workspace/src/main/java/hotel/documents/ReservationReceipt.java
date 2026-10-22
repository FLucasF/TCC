package hotel.documents;

import hotel.reservations.Guest;
import hotel.reservations.PriceCalculator;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The document the guest gets when the reservation is confirmed, at check-out, and on request: a receipt
 * for a guest with a CPF, an invoice for a company, a booking receipt (in English) for a foreign guest.
 * The fields that identify the guest in the other kinds of document are null.
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
                titleFor(guest),
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
                textFor(guest, reservation, room));
    }

    private static boolean isCompany(Guest guest) {
        return guest.companyCnpj() != null;
    }

    private static boolean isForeigner(Guest guest) {
        return guest.passport() != null;
    }

    private static String titleFor(Guest guest) {
        if (isCompany(guest)) {
            return "Fatura de hospedagem";
        }
        return isForeigner(guest) ? "Booking receipt" : "Comprovante de reserva";
    }

    private static String textFor(Guest guest, Reservation reservation, Room room) {
        if (isForeigner(guest)) {
            return englishText(guest, reservation, room);
        }
        return portugueseText(guest, reservation, room);
    }

    private static String portugueseText(Guest guest, Reservation reservation, Room room) {
        boolean company = isCompany(guest);
        var price = reservation.price();
        StringBuilder text = new StringBuilder();
        text.append(titleFor(guest)).append(" nº ").append(reservation.id()).append('\n');
        if (company) {
            text.append("Empresa: ").append(guest.companyName())
                    .append(" (CNPJ ").append(guest.companyCnpj()).append(")\n");
            text.append("Hóspede: ").append(guest.name()).append('\n');
        } else {
            text.append("Hóspede: ").append(guest.name()).append(" (CPF ").append(guest.cpf()).append(")\n");
        }
        text.append("Quarto: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        int nights = price.nights();
        text.append("Entrada: ").append(TextFormat.date(reservation.checkIn()))
                .append(" | Saída: ").append(TextFormat.date(reservation.checkOut()))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(price.subtotal())).append('\n');
        if (price.discount().signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(price.discount())).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(price.total())).append('\n');
        if (company) {
            BigDecimal iss = PriceCalculator.cents(price.total().multiply(WITHHELD_ISS_RATE));
            text.append("ISS retido (5%): ").append(TextFormat.money(iss)).append('\n');
        }
        return text.toString();
    }

    private static String englishText(Guest guest, Reservation reservation, Room room) {
        var price = reservation.price();
        StringBuilder text = new StringBuilder();
        text.append(titleFor(guest)).append(" no. ").append(reservation.id()).append('\n');
        text.append("Guest: ").append(guest.name())
                .append(" (passport ").append(guest.passport()).append(", ").append(guest.country()).append(")\n");
        text.append("Room: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        int nights = price.nights();
        text.append("Check-in: ").append(TextFormat.isoDate(reservation.checkIn()))
                .append(" | Check-out: ").append(TextFormat.isoDate(reservation.checkOut()))
                .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(TextFormat.englishMoney(price.subtotal())).append('\n');
        if (price.discount().signum() > 0) {
            text.append("Discount: ").append(TextFormat.englishMoney(price.discount())).append('\n');
        }
        text.append("Total: ").append(TextFormat.englishMoney(price.total())).append('\n');
        return text.toString();
    }
}

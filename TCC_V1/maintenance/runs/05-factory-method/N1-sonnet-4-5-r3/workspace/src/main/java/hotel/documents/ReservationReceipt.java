package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;

/** The receipt the guest gets when the reservation is confirmed, at check-out, and on request. */
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

    public ReservationReceipt(Reservation reservation, Room room) {
        this(
                documentTitle(reservation),
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
                reservation.price().total(),
                generateText(
                        documentTitle(reservation),
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
                        reservation.price().total()));
    }

    private static String documentTitle(Reservation reservation) {
        if (reservation.guest().isCpf()) {
            return "Comprovante de reserva";
        } else if (reservation.guest().isCompany()) {
            return "Fatura de hospedagem";
        } else {
            return "Booking receipt";
        }
    }

    private static String generateText(
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
        if (companyCnpj != null) {
            return invoiceText(
                    title, reservationId, guestName, companyCnpj, companyName, roomNumber, roomType,
                    checkIn, checkOut, nights, subtotal, discount, total);
        } else if (guestPassport != null) {
            return bookingReceiptText(
                    title, reservationId, guestName, guestPassport, guestCountry, roomNumber, roomType,
                    checkIn, checkOut, nights, subtotal, discount, total);
        } else {
            return receiptText(
                    title, reservationId, guestName, guestCpf, roomNumber, roomType,
                    checkIn, checkOut, nights, subtotal, discount, total);
        }
    }

    private static String receiptText(
            String title,
            long reservationId,
            String guestName,
            String guestCpf,
            int roomNumber,
            RoomType roomType,
            LocalDate checkIn,
            LocalDate checkOut,
            int nights,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total) {
        StringBuilder text = new StringBuilder();
        text.append(title).append(" nº ").append(reservationId).append('\n');
        text.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(checkIn))
                .append(" | Saída: ").append(TextFormat.date(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(discount)).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(total)).append('\n');
        return text.toString();
    }

    private static String invoiceText(
            String title,
            long reservationId,
            String guestName,
            String companyCnpj,
            String companyName,
            int roomNumber,
            RoomType roomType,
            LocalDate checkIn,
            LocalDate checkOut,
            int nights,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total) {
        StringBuilder text = new StringBuilder();
        text.append(title).append(" nº ").append(reservationId).append('\n');
        text.append("Empresa: ").append(companyName).append(" (CNPJ ").append(companyCnpj).append(")\n");
        text.append("Hóspede: ").append(guestName).append('\n');
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(checkIn))
                .append(" | Saída: ").append(TextFormat.date(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(discount)).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(total)).append('\n');
        BigDecimal iss = total.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
        text.append("ISS retido (5%): ").append(TextFormat.money(iss)).append('\n');
        return text.toString();
    }

    private static String bookingReceiptText(
            String title,
            long reservationId,
            String guestName,
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
        StringBuilder text = new StringBuilder();
        text.append(title).append(" no. ").append(reservationId).append('\n');
        text.append("Guest: ").append(guestName).append(" (passport ").append(guestPassport)
                .append(", ").append(guestCountry).append(")\n");
        text.append("Room: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Check-in: ").append(checkIn)
                .append(" | Check-out: ").append(checkOut)
                .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(englishMoney(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Discount: ").append(englishMoney(discount)).append('\n');
        }
        text.append("Total: ").append(englishMoney(total)).append('\n');
        return text.toString();
    }

    private static String englishMoney(BigDecimal amount) {
        return String.format(Locale.US, "BRL %,.2f", amount);
    }
}

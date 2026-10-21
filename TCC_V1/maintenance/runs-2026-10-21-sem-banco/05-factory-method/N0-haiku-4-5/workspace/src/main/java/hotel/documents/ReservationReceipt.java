package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A receipt document for reservations. Can be CPF, company invoice, or booking receipt depending on the guest type. */
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
        String text) implements Document {

    public ReservationReceipt(Reservation reservation, Room room) {
        this(
                determineTitleAndReturn(reservation),
                reservation.id(),
                reservation.guest().name(),
                reservation.guest().cpf(),
                reservation.guest().companyCnpj(),
                reservation.guest().companyName(),
                reservation.guest().guestPassport(),
                reservation.guest().guestCountry(),
                room.number(),
                room.type(),
                reservation.checkIn(),
                reservation.checkOut(),
                reservation.price().nights(),
                reservation.price().subtotal(),
                reservation.price().discount(),
                reservation.price().total(),
                computeDocumentText(
                        determineTitleAndReturn(reservation),
                        reservation.id(),
                        reservation.guest().name(),
                        reservation.guest().cpf(),
                        reservation.guest().companyCnpj(),
                        reservation.guest().companyName(),
                        reservation.guest().guestPassport(),
                        reservation.guest().guestCountry(),
                        room.number(),
                        room.type(),
                        reservation.checkIn(),
                        reservation.checkOut(),
                        reservation.price().nights(),
                        reservation.price().subtotal(),
                        reservation.price().discount(),
                        reservation.price().total()));
    }

    private static String computeDocumentText(String title, long reservationId, String guestName, String guestCpf,
            String companyCnpj, String companyName, String guestPassport, String guestCountry, int roomNumber,
            RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights, BigDecimal subtotal,
            BigDecimal discount, BigDecimal total) {
        if (guestPassport != null) {
            return formatBookingReceipt(title, reservationId, guestName, guestPassport, guestCountry, roomNumber,
                    roomType, checkIn, checkOut, nights, subtotal, discount, total);
        } else if (companyCnpj != null) {
            return formatCompanyInvoice(title, reservationId, guestName, companyName, companyCnpj, roomNumber,
                    roomType, checkIn, checkOut, nights, subtotal, discount, total);
        } else {
            return formatCpfReceipt(title, reservationId, guestName, guestCpf, roomNumber, roomType, checkIn,
                    checkOut, nights, subtotal, discount, total);
        }
    }

    private static String determineTitleAndReturn(Reservation reservation) {
        if (reservation.guest().guestPassport() != null) {
            return "Booking receipt";
        } else if (reservation.guest().companyCnpj() != null) {
            return "Fatura de hospedagem";
        } else {
            return "Comprovante de reserva";
        }
    }

    private static String formatCpfReceipt(String title, long reservationId, String guestName, String guestCpf,
            int roomNumber, RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights,
            BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
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

    private static String formatCompanyInvoice(String title, long reservationId, String guestName, String companyName,
            String companyCnpj, int roomNumber, RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights,
            BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        BigDecimal iss = calculateISS(total);
        StringBuilder text = new StringBuilder();
        text.append(title).append(" nº ").append(reservationId).append('\n');
        text.append("Empresa: ").append(companyName).append(" (CNPJ ").append(companyCnpj).append(")\n");
        text.append("Hóspede: ").append(guestName).append('\n');
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(checkIn))
                .append(" | Saída: ").append(TextFormat.date(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(subtotal)).append('\n');
        text.append("Total: ").append(TextFormat.money(total)).append('\n');
        text.append("ISS retido (5%): ").append(TextFormat.money(iss)).append('\n');
        return text.toString();
    }

    private static String formatBookingReceipt(String title, long reservationId, String guestName,
            String guestPassport, String guestCountry, int roomNumber, RoomType roomType, LocalDate checkIn,
            LocalDate checkOut, int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        StringBuilder text = new StringBuilder();
        text.append(title).append(" no. ").append(reservationId).append('\n');
        text.append("Guest: ").append(guestName).append(" (passport ").append(guestPassport)
                .append(", ").append(guestCountry).append(")\n");
        text.append("Room: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Check-in: ").append(EnglishFormat.date(checkIn))
                .append(" | Check-out: ").append(EnglishFormat.date(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(EnglishFormat.money(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Discount: ").append(EnglishFormat.money(discount)).append('\n');
        }
        text.append("Total: ").append(EnglishFormat.money(total)).append('\n');
        return text.toString();
    }

    private static BigDecimal calculateISS(BigDecimal total) {
        return total.multiply(BigDecimal.valueOf(0.05))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }
}

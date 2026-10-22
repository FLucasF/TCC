package hotel.documents;

import hotel.reservations.GuestType;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

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
                titleFor(reservation.guest().type()),
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
                        reservation.guest().type(),
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

    private static String titleFor(GuestType guestType) {
        return switch (guestType) {
            case INDIVIDUAL -> "Comprovante de reserva";
            case COMPANY -> "Fatura de hospedagem";
            case FOREIGN -> "Booking receipt";
        };
    }

    private static String generateText(
            GuestType guestType,
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
        return switch (guestType) {
            case INDIVIDUAL -> generateIndividualReceipt(
                    reservationId, guestName, guestCpf, roomNumber, roomType, checkIn, checkOut, nights, subtotal,
                    discount, total);
            case COMPANY -> generateCompanyInvoice(
                    reservationId, companyName, companyCnpj, guestName, roomNumber, roomType, checkIn, checkOut,
                    nights, subtotal, discount, total);
            case FOREIGN -> generateForeignReceipt(
                    reservationId, guestName, guestPassport, guestCountry, roomNumber, roomType, checkIn, checkOut,
                    nights, subtotal, discount, total);
        };
    }

    private static String generateIndividualReceipt(
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
        text.append("Comprovante de reserva nº ").append(reservationId).append('\n');
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

    private static String generateCompanyInvoice(
            long reservationId,
            String companyName,
            String companyCnpj,
            String guestName,
            int roomNumber,
            RoomType roomType,
            LocalDate checkIn,
            LocalDate checkOut,
            int nights,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total) {
        StringBuilder text = new StringBuilder();
        text.append("Fatura de hospedagem nº ").append(reservationId).append('\n');
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
        BigDecimal iss = total.multiply(new BigDecimal("0.05")).setScale(2, java.math.RoundingMode.HALF_EVEN);
        text.append("ISS retido (5%): ").append(TextFormat.money(iss)).append('\n');
        return text.toString();
    }

    private static String generateForeignReceipt(
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
        text.append("Booking receipt no. ").append(reservationId).append('\n');
        text.append("Guest: ").append(guestName)
                .append(" (passport ").append(guestPassport)
                .append(", ").append(guestCountry).append(")\n");
        text.append("Room: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Check-in: ").append(TextFormat.isoDate(checkIn))
                .append(" | Check-out: ").append(TextFormat.isoDate(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(TextFormat.usMoney(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Discount: ").append(TextFormat.usMoney(discount)).append('\n');
        }
        text.append("Total: ").append(TextFormat.usMoney(total)).append('\n');
        return text.toString();
    }
}

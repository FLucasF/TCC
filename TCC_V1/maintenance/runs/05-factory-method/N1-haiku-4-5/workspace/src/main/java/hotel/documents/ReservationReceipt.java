package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
                "Comprovante de reserva",
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
                generateText(reservation, room));
    }

    private static String generateText(Reservation reservation, Room room) {
        if (reservation.guest().cpf() != null) {
            return generateReceipt(
                    reservation.id(),
                    reservation.guest().name(),
                    reservation.guest().cpf(),
                    room.number(),
                    room.type(),
                    reservation.checkIn(),
                    reservation.checkOut(),
                    reservation.price().nights(),
                    reservation.price().subtotal(),
                    reservation.price().discount(),
                    reservation.price().total());
        } else if (reservation.guest().companyCnpj() != null) {
            return generateInvoice(
                    reservation.id(),
                    reservation.guest().companyName(),
                    reservation.guest().companyCnpj(),
                    reservation.guest().name(),
                    room.number(),
                    room.type(),
                    reservation.checkIn(),
                    reservation.checkOut(),
                    reservation.price().nights(),
                    reservation.price().subtotal(),
                    reservation.price().discount(),
                    reservation.price().total());
        } else if (reservation.guest().guestPassport() != null) {
            return generateBookingReceipt(
                    reservation.id(),
                    reservation.guest().name(),
                    reservation.guest().guestPassport(),
                    reservation.guest().guestCountry(),
                    room.number(),
                    room.type(),
                    reservation.checkIn(),
                    reservation.checkOut(),
                    reservation.price().nights(),
                    reservation.price().subtotal(),
                    reservation.price().discount(),
                    reservation.price().total());
        }
        return "";
    }

    private static String generateReceipt(
            long id,
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
        text.append("Comprovante de reserva nº ").append(id).append('\n');
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

    private static String generateInvoice(
            long id,
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
        BigDecimal iss = total.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
        StringBuilder text = new StringBuilder();
        text.append("Fatura de hospedagem nº ").append(id).append('\n');
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
        text.append("ISS retido (5%): ").append(TextFormat.money(iss)).append('\n');
        return text.toString();
    }

    private static String generateBookingReceipt(
            long id,
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
        text.append("Booking receipt no. ").append(id).append('\n');
        text.append("Guest: ").append(guestName).append(" (passport ").append(guestPassport)
                .append(", ").append(guestCountry).append(")\n");
        text.append("Room: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Check-in: ").append(TextFormat.dateEnglish(checkIn))
                .append(" | Check-out: ").append(TextFormat.dateEnglish(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(TextFormat.moneyEnglish(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Discount: ").append(TextFormat.moneyEnglish(discount)).append('\n');
        }
        text.append("Total: ").append(TextFormat.moneyEnglish(total)).append('\n');
        return text.toString();
    }
}

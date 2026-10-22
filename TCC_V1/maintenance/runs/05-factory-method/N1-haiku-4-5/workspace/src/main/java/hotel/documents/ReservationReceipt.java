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
                titleFor(reservation),
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

    private static String titleFor(Reservation reservation) {
        if (reservation.guest().companyCnpj() != null) {
            return "Fatura de hospedagem";
        } else if (reservation.guest().guestPassport() != null) {
            return "Booking receipt";
        } else {
            return "Comprovante de reserva";
        }
    }

    private static String generateText(Reservation reservation, Room room) {
        String companyCnpj = reservation.guest().companyCnpj();
        String companyName = reservation.guest().companyName();
        String guestPassport = reservation.guest().guestPassport();
        String guestCountry = reservation.guest().guestCountry();

        if (companyCnpj != null) {
            return companyInvoiceTextFor(
                    titleFor(reservation),
                    reservation.id(),
                    companyName,
                    companyCnpj,
                    reservation.guest().name(),
                    room.number(),
                    room.type(),
                    reservation.checkIn(),
                    reservation.checkOut(),
                    reservation.price().nights(),
                    reservation.price().subtotal(),
                    reservation.price().total());
        } else if (guestPassport != null) {
            return bookingReceiptTextFor(
                    titleFor(reservation),
                    reservation.id(),
                    reservation.guest().name(),
                    guestPassport,
                    guestCountry,
                    room.number(),
                    room.type(),
                    reservation.checkIn(),
                    reservation.checkOut(),
                    reservation.price().nights(),
                    reservation.price().subtotal(),
                    reservation.price().discount(),
                    reservation.price().total());
        } else {
            return receiptTextFor(
                    titleFor(reservation),
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
        }
    }

    private static String receiptTextFor(
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

    private static String companyInvoiceTextFor(
            String title,
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
            BigDecimal total) {
        BigDecimal issRetained = total.multiply(BigDecimal.valueOf(0.05));
        issRetained = issRetained.setScale(2, RoundingMode.HALF_EVEN);

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
        text.append("ISS retido (5%): ").append(TextFormat.money(issRetained)).append('\n');
        return text.toString();
    }

    private static String bookingReceiptTextFor(
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
        text.append("Check-in: ").append(TextFormat.isoDate(checkIn))
                .append(" | Check-out: ").append(TextFormat.isoDate(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(TextFormat.brlMoney(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Discount: ").append(TextFormat.brlMoney(discount)).append('\n');
        }
        text.append("Total: ").append(TextFormat.brlMoney(total)).append('\n');
        return text.toString();
    }
}

package hotel.documents;

import hotel.reservations.Guest;
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
        this(reservation, room, generateText(
                titleFor(reservation.guest()),
                reservation.id(),
                reservation.guest(),
                room.number(),
                room.type(),
                reservation.checkIn(),
                reservation.checkOut(),
                reservation.price().nights(),
                reservation.price().subtotal(),
                reservation.price().discount(),
                reservation.price().total()));
    }

    private ReservationReceipt(Reservation reservation, Room room, String generatedText) {
        this(
                titleFor(reservation.guest()),
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
                generatedText);
    }

    private static String titleFor(Guest guest) {
        return switch (guest.type()) {
            case INDIVIDUAL -> "Comprovante de reserva";
            case COMPANY -> "Fatura de hospedagem";
            case FOREIGN -> "Booking receipt";
        };
    }

    private static String generateText(
            String title,
            long reservationId,
            Guest guest,
            int roomNumber,
            RoomType roomType,
            LocalDate checkIn,
            LocalDate checkOut,
            int nights,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total) {
        return switch (guest.type()) {
            case INDIVIDUAL -> generateIndividualReceipt(
                    title, reservationId, guest, roomNumber, roomType, checkIn, checkOut, nights, subtotal, discount,
                    total);
            case COMPANY -> generateCompanyInvoice(
                    title, reservationId, guest, roomNumber, roomType, checkIn, checkOut, nights, subtotal, total);
            case FOREIGN -> generateForeignReceipt(
                    title, reservationId, guest, roomNumber, roomType, checkIn, checkOut, nights, subtotal, discount,
                    total);
        };
    }

    private static String generateIndividualReceipt(
            String title,
            long reservationId,
            Guest guest,
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
        text.append("Hóspede: ").append(guest.name()).append(" (CPF ").append(guest.cpf()).append(")\n");
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
            String title,
            long reservationId,
            Guest guest,
            int roomNumber,
            RoomType roomType,
            LocalDate checkIn,
            LocalDate checkOut,
            int nights,
            BigDecimal subtotal,
            BigDecimal total) {
        StringBuilder text = new StringBuilder();
        text.append(title).append(" nº ").append(reservationId).append('\n');
        text.append("Empresa: ").append(guest.companyName())
                .append(" (CNPJ ").append(guest.companyCnpj()).append(")\n");
        text.append("Hóspede: ").append(guest.name()).append('\n');
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(checkIn))
                .append(" | Saída: ").append(TextFormat.date(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(subtotal)).append('\n');
        text.append("Total: ").append(TextFormat.money(total)).append('\n');
        BigDecimal iss = total.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
        text.append("ISS retido (5%): ").append(TextFormat.money(iss)).append('\n');
        return text.toString();
    }

    private static String generateForeignReceipt(
            String title,
            long reservationId,
            Guest guest,
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
        text.append("Guest: ").append(guest.name())
                .append(" (passport ").append(guest.passport())
                .append(", ").append(guest.country()).append(")\n");
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

    public String text() {
        return text;
    }
}

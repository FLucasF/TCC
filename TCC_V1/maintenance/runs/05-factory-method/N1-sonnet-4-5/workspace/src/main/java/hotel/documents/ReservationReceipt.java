package hotel.documents;

import hotel.reservations.Guest;
import hotel.reservations.PriceCalculator;
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
        this(reservation.guest(), reservation, room);
    }

    private ReservationReceipt(Guest guest, Reservation reservation, Room room) {
        this(
                title(guest),
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
                generateText(
                        title(guest),
                        reservation.id(),
                        guest,
                        room.number(),
                        room.type(),
                        reservation.checkIn(),
                        reservation.checkOut(),
                        reservation.price().nights(),
                        reservation.price().subtotal(),
                        reservation.price().discount(),
                        reservation.price().total()));
    }

    private static String title(Guest guest) {
        if (guest.companyCnpj() != null) {
            return "Fatura de hospedagem";
        } else if (guest.passport() != null) {
            return "Booking receipt";
        } else {
            return "Comprovante de reserva";
        }
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

        if (guest.companyCnpj() != null) {
            return companyInvoiceText(
                    reservationId, guest, roomNumber, roomType, checkIn, checkOut, nights, subtotal, discount, total);
        } else if (guest.passport() != null) {
            return bookingReceiptText(
                    reservationId, guest, roomNumber, roomType, checkIn, checkOut, nights, subtotal, discount, total);
        } else {
            return personalReceiptText(
                    reservationId, guest, roomNumber, roomType, checkIn, checkOut, nights, subtotal, discount, total);
        }
    }

    private static String personalReceiptText(
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
        text.append("Comprovante de reserva nº ").append(reservationId).append('\n');
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

    private static String companyInvoiceText(
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
        text.append("Fatura de hospedagem nº ").append(reservationId).append('\n');
        text.append("Empresa: ").append(guest.companyName())
                .append(" (CNPJ ").append(guest.companyCnpj()).append(")\n");
        text.append("Hóspede: ").append(guest.name()).append('\n');
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(checkIn))
                .append(" | Saída: ").append(TextFormat.date(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(discount)).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(total)).append('\n');
        BigDecimal iss = PriceCalculator.cents(total.multiply(new BigDecimal("0.05")));
        text.append("ISS retido (5%): ").append(TextFormat.money(iss)).append('\n');
        return text.toString();
    }

    private static String bookingReceiptText(
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
        text.append("Booking receipt no. ").append(reservationId).append('\n');
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
}

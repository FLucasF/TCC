package hotel.documents;

import hotel.reservations.Price;
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
        BigDecimal issRetained,
        String text) {

    public ReservationReceipt(Reservation reservation, Room room) {
        this(generateReceipt(reservation, room));
    }

    private ReservationReceipt(ReservationReceipt receipt) {
        this(
                receipt.title,
                receipt.reservationId,
                receipt.guestName,
                receipt.guestCpf,
                receipt.companyCnpj,
                receipt.companyName,
                receipt.guestPassport,
                receipt.guestCountry,
                receipt.roomNumber,
                receipt.roomType,
                receipt.checkIn,
                receipt.checkOut,
                receipt.nights,
                receipt.subtotal,
                receipt.discount,
                receipt.total,
                receipt.issRetained,
                receipt.text);
    }

    private static ReservationReceipt generateReceipt(Reservation reservation, Room room) {
        String title;
        BigDecimal issRetained;
        String text;

        boolean isCpf = reservation.guest().cpf() != null;
        boolean isCompany = reservation.guest().companyCnpj() != null;
        boolean isPassport = reservation.guest().guestPassport() != null;

        if (isCpf) {
            title = "Comprovante de reserva";
            issRetained = null;
            text = generateBrazilianReceipt(reservation, room);
        } else if (isCompany) {
            title = "Fatura de hospedagem";
            issRetained = reservation.price().total().multiply(new BigDecimal("0.05"))
                    .setScale(2, java.math.RoundingMode.HALF_EVEN);
            text = generateInvoice(reservation, room, issRetained);
        } else if (isPassport) {
            title = "Booking receipt";
            issRetained = null;
            text = generateBookingReceipt(reservation, room);
        } else {
            throw new IllegalStateException("reservation has no identification");
        }

        return new ReservationReceipt(
                title,
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
                issRetained,
                text);
    }

    private static String generateBrazilianReceipt(Reservation reservation, Room room) {
        StringBuilder text = new StringBuilder();
        text.append("Comprovante de reserva nº ").append(reservation.id()).append('\n');
        text.append("Hóspede: ").append(reservation.guest().name())
                .append(" (CPF ").append(reservation.guest().cpf()).append(")\n");
        text.append("Quarto: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        Price price = reservation.price();
        text.append("Entrada: ").append(TextFormat.date(reservation.checkIn()))
                .append(" | Saída: ").append(TextFormat.date(reservation.checkOut()))
                .append(" | ").append(price.nights()).append(price.nights() == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(price.subtotal())).append('\n');
        if (price.discount().signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(price.discount())).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(price.total())).append('\n');
        return text.toString();
    }

    private static String generateInvoice(Reservation reservation, Room room, BigDecimal issRetained) {
        StringBuilder text = new StringBuilder();
        text.append("Fatura de hospedagem nº ").append(reservation.id()).append('\n');
        text.append("Empresa: ").append(reservation.guest().companyName())
                .append(" (CNPJ ").append(reservation.guest().companyCnpj()).append(")\n");
        text.append("Hóspede: ").append(reservation.guest().name()).append('\n');
        text.append("Quarto: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        Price price = reservation.price();
        text.append("Entrada: ").append(TextFormat.date(reservation.checkIn()))
                .append(" | Saída: ").append(TextFormat.date(reservation.checkOut()))
                .append(" | ").append(price.nights()).append(price.nights() == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(price.subtotal())).append('\n');
        if (price.discount().signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(price.discount())).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(price.total())).append('\n');
        text.append("ISS retido (5%): ").append(TextFormat.money(issRetained)).append('\n');
        return text.toString();
    }

    private static String generateBookingReceipt(Reservation reservation, Room room) {
        StringBuilder text = new StringBuilder();
        text.append("Booking receipt no. ").append(reservation.id()).append('\n');
        text.append("Guest: ").append(reservation.guest().name())
                .append(" (passport ").append(reservation.guest().guestPassport())
                .append(", ").append(reservation.guest().guestCountry()).append(")\n");
        text.append("Room: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        Price price = reservation.price();
        text.append("Check-in: ").append(TextFormat.dateInEnglish(reservation.checkIn()))
                .append(" | Check-out: ").append(TextFormat.dateInEnglish(reservation.checkOut()))
                .append(" | ").append(price.nights()).append(price.nights() == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(TextFormat.moneyInEnglish(price.subtotal())).append('\n');
        if (price.discount().signum() > 0) {
            text.append("Discount: ").append(TextFormat.moneyInEnglish(price.discount())).append('\n');
        }
        text.append("Total: ").append(TextFormat.moneyInEnglish(price.total())).append('\n');
        return text.toString();
    }
}

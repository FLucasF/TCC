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
                generateText(reservation, room));
    }

    private static String titleFor(Guest guest) {
        return switch (guest.type()) {
            case INDIVIDUAL -> "Comprovante de reserva";
            case COMPANY -> "Fatura de hospedagem";
            case FOREIGNER -> "Booking receipt";
        };
    }

    private static String generateText(Reservation reservation, Room room) {
        Guest guest = reservation.guest();
        return switch (guest.type()) {
            case INDIVIDUAL -> generateIndividualReceipt(reservation, room);
            case COMPANY -> generateCompanyInvoice(reservation, room);
            case FOREIGNER -> generateForeignerReceipt(reservation, room);
        };
    }

    private static String generateIndividualReceipt(Reservation reservation, Room room) {
        StringBuilder text = new StringBuilder();
        text.append("Comprovante de reserva nº ").append(reservation.id()).append('\n');
        text.append("Hóspede: ").append(reservation.guest().name())
                .append(" (CPF ").append(reservation.guest().cpf()).append(")\n");
        text.append("Quarto: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(reservation.checkIn()))
                .append(" | Saída: ").append(TextFormat.date(reservation.checkOut()))
                .append(" | ").append(reservation.price().nights())
                .append(reservation.price().nights() == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(reservation.price().subtotal())).append('\n');
        if (reservation.price().discount().signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(reservation.price().discount())).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(reservation.price().total())).append('\n');
        return text.toString();
    }

    private static String generateCompanyInvoice(Reservation reservation, Room room) {
        BigDecimal issRetained = reservation.price().total()
                .multiply(new BigDecimal("0.05"))
                .setScale(2, RoundingMode.HALF_EVEN);

        StringBuilder text = new StringBuilder();
        text.append("Fatura de hospedagem nº ").append(reservation.id()).append('\n');
        text.append("Empresa: ").append(reservation.guest().companyName())
                .append(" (CNPJ ").append(reservation.guest().companyCnpj()).append(")\n");
        text.append("Hóspede: ").append(reservation.guest().name()).append('\n');
        text.append("Quarto: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(reservation.checkIn()))
                .append(" | Saída: ").append(TextFormat.date(reservation.checkOut()))
                .append(" | ").append(reservation.price().nights())
                .append(reservation.price().nights() == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(reservation.price().subtotal())).append('\n');
        if (reservation.price().discount().signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(reservation.price().discount())).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(reservation.price().total())).append('\n');
        text.append("ISS retido (5%): ").append(TextFormat.money(issRetained)).append('\n');
        return text.toString();
    }

    private static String generateForeignerReceipt(Reservation reservation, Room room) {
        StringBuilder text = new StringBuilder();
        text.append("Booking receipt no. ").append(reservation.id()).append('\n');
        text.append("Guest: ").append(reservation.guest().name())
                .append(" (passport ").append(reservation.guest().passport())
                .append(", ").append(reservation.guest().country()).append(")\n");
        text.append("Room: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        text.append("Check-in: ").append(TextFormat.dateIso(reservation.checkIn()))
                .append(" | Check-out: ").append(TextFormat.dateIso(reservation.checkOut()))
                .append(" | ").append(reservation.price().nights())
                .append(reservation.price().nights() == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(TextFormat.moneyEnglish(reservation.price().subtotal())).append('\n');
        if (reservation.price().discount().signum() > 0) {
            text.append("Discount: ").append(TextFormat.moneyEnglish(reservation.price().discount())).append('\n');
        }
        text.append("Total: ").append(TextFormat.moneyEnglish(reservation.price().total())).append('\n');
        return text.toString();
    }
}

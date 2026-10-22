package hotel.documents;

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
        StringBuilder text = new StringBuilder();
        text.append("Comprovante de reserva nº ").append(reservation.id()).append('\n');
        text.append("Hóspede: ").append(reservation.guest().name()).append(" (CPF ").append(reservation.guest().cpf()).append(")\n");
        text.append("Quarto: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(reservation.checkIn()))
                .append(" | Saída: ").append(TextFormat.date(reservation.checkOut()))
                .append(" | ").append(reservation.price().nights()).append(reservation.price().nights() == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(reservation.price().subtotal())).append('\n');
        if (reservation.price().discount().signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(reservation.price().discount())).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(reservation.price().total())).append('\n');
        return text.toString();
    }
}

package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class BookingReceipt extends Document {

    private static final Locale US = Locale.US;
    private static final DateTimeFormatter US_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", US);

    public BookingReceipt(Reservation reservation, Room room) {
        super(
                reservation.id(),
                reservation.guest().name(),
                reservation.guest().cpf(),
                reservation.guest().companyCnpj(),
                reservation.guest().companyName(),
                reservation.guest().guestPassport(),
                reservation.guest().guestCountry(),
                room.number(),
                room.type().name(),
                reservation.checkIn(),
                reservation.checkOut(),
                reservation.price().nights(),
                reservation.price().subtotal(),
                reservation.price().discount(),
                reservation.price().total());
    }

    @Override
    public String text() {
        StringBuilder text = new StringBuilder();
        text.append(title()).append(" no. ").append(reservationId).append('\n');
        text.append("Guest: ").append(guestName).append(" (passport ").append(guestPassport)
                .append(", ").append(guestCountry).append(")\n");
        text.append("Room: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Check-in: ").append(US_DATE.format(checkIn))
                .append(" | Check-out: ").append(US_DATE.format(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(formatMoney(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Discount: ").append(formatMoney(discount)).append('\n');
        }
        text.append("Total: ").append(formatMoney(total)).append('\n');
        return text.toString();
    }

    private String formatMoney(BigDecimal amount) {
        return "BRL " + String.format(US, "%,.2f", amount);
    }
}

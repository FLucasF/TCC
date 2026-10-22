package hotel.documents;

import hotel.reservations.ForeignGuest;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

/** The booking receipt for foreign guests (passport). */
public record BookingReceipt(
        long reservationId,
        String guestName,
        String guestPassport,
        String guestCountry,
        int roomNumber,
        String roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total) implements ReservationDocument {

    public BookingReceipt(Reservation reservation, Room room) {
        this(
                reservation.id(),
                reservation.guest().name(),
                ((ForeignGuest) reservation.guest()).passport(),
                ((ForeignGuest) reservation.guest()).country(),
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
    public String title() {
        return "Booking receipt";
    }

    @Override
    public String guestCpf() {
        return null;
    }

    @Override
    public String companyCnpj() {
        return null;
    }

    @Override
    public String companyName() {
        return null;
    }

    @Override
    public String text() {
        StringBuilder text = new StringBuilder();
        text.append(title()).append(" no. ").append(reservationId).append('\n');
        text.append("Guest: ").append(guestName)
                .append(" (passport ").append(guestPassport)
                .append(", ").append(guestCountry).append(")\n");
        text.append("Room: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Check-in: ").append(checkIn)
                .append(" | Check-out: ").append(checkOut)
                .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(formatMoney(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Discount: ").append(formatMoney(discount)).append('\n');
        }
        text.append("Total: ").append(formatMoney(total)).append('\n');
        return text.toString();
    }

    private static String formatMoney(BigDecimal amount) {
        return "BRL " + String.format(Locale.US, "%,.2f", amount);
    }
}

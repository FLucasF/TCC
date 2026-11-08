package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The tourism tax owed for reservations that start in a given month (CSV).
 * The city charges R$ 4.50 per guest per night.
 */
public final class TourismTaxReport {

    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final ReservationStore store;

    public TourismTaxReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        List<Reservation> paidReservations = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (reservation.isPaid() && startsInMonth) {
                paidReservations.add(reservation);
            }
        }
        paidReservations.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));

        StringBuilder dataLines = new StringBuilder();
        BigDecimal totalTax = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : paidReservations) {
            BigDecimal tax = TAX_PER_GUEST_PER_NIGHT
                    .multiply(new BigDecimal(reservation.guests()))
                    .multiply(new BigDecimal(reservation.price().nights()))
                    .setScale(2);
            dataLines.append(reservation.id()).append(',')
                    .append(reservation.roomNumber()).append(',')
                    .append(reservation.checkIn()).append(',')
                    .append(reservation.checkOut()).append(',')
                    .append(reservation.guests()).append(',')
                    .append(reservation.price().nights()).append(',')
                    .append(tax.toPlainString()).append('\n');
            totalTax = totalTax.add(tax);
        }

        String header = "reservation,room,check_in,check_out,guests,nights,tax";
        String totalLine = "total," + totalTax.toPlainString();
        return new CsvReport("tourism-tax", month.toString()).format(header, dataLines.toString(), totalLine);
    }
}

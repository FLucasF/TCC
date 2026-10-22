package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The city's tourism tax, per guest per night, on the paid reservations that start in a month (CSV). */
public final class TourismTaxReport {

    /** The tax in force; the city changes it every year. */
    public static final BigDecimal DEFAULT_TAX_PER_GUEST_NIGHT = new BigDecimal("4.50");

    private final ReservationStore store;
    private final BigDecimal taxPerGuestNight;

    public TourismTaxReport(ReservationStore store, BigDecimal taxPerGuestNight) {
        this.store = store;
        this.taxPerGuestNight = taxPerGuestNight;
    }

    public String generate(YearMonth month) {
        List<Reservation> taxed = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            if (reservation.isPaid() && YearMonth.from(reservation.checkIn()).equals(month)) {
                taxed.add(reservation);
            }
        }
        taxed.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));

        List<String> rows = new ArrayList<>();
        BigDecimal total = money(BigDecimal.ZERO);
        for (Reservation reservation : taxed) {
            int nights = reservation.price().nights();
            BigDecimal tax = money(taxPerGuestNight.multiply(BigDecimal.valueOf((long) reservation.guests() * nights)));
            rows.add(reservation.id() + "," + reservation.roomNumber() + "," + reservation.checkIn() + ","
                    + reservation.checkOut() + "," + reservation.guests() + "," + nights + ","
                    + tax.toPlainString());
            total = total.add(tax);
        }
        return CsvLayout.render("tourism-tax", month.toString(),
                "reservation,room,check_in,check_out,guests,nights,tax", rows, "total," + total.toPlainString());
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }
}

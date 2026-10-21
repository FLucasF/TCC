package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** The city's tourism tax on the paid reservations that start in a given month (CSV). */
public final class TourismTaxReport {

    private final ReservationStore store;
    private final BigDecimal taxPerGuestNight;

    public TourismTaxReport(ReservationStore store, BigDecimal taxPerGuestNight) {
        this.store = store;
        this.taxPerGuestNight = taxPerGuestNight;
    }

    public String generate(YearMonth month) {
        List<String> rows = new ArrayList<>();
        BigDecimal total = money(BigDecimal.ZERO);
        for (Reservation reservation : MonthlyReservations.startingIn(month, store, Reservation::isPaid)) {
            long nights = ChronoUnit.DAYS.between(reservation.checkIn(), reservation.checkOut());
            BigDecimal tax = money(taxPerGuestNight.multiply(BigDecimal.valueOf(reservation.guests() * nights)));
            rows.add(reservation.id() + "," + reservation.roomNumber() + "," + reservation.checkIn() + ","
                    + reservation.checkOut() + "," + reservation.guests() + "," + nights + ","
                    + tax.toPlainString());
            total = total.add(tax);
        }
        return CsvLayout.render("tourism-tax", month.toString(),
                "reservation,room,check_in,check_out,guests,nights,tax", rows, "total," + total.toPlainString());
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}

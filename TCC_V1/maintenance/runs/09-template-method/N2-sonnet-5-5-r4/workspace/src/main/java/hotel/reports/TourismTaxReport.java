package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** The city's tourism tax, per guest per night, on the paid reservations that start in a given month (CSV). */
public final class TourismTaxReport extends Report<YearMonth> {

    private final ReservationStore store;
    private final BigDecimal taxPerGuestNight;

    public TourismTaxReport(ReservationStore store, BigDecimal taxPerGuestNight) {
        this.store = store;
        this.taxPerGuestNight = taxPerGuestNight;
    }

    @Override
    protected String name() {
        return "tourism-tax";
    }

    @Override
    protected String header() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected Content content(YearMonth month) {
        List<Reservation> taxed = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            if (reservation.isPaid() && YearMonth.from(reservation.checkIn()).equals(month)) {
                taxed.add(reservation);
            }
        }
        taxed.sort(BY_CHECK_IN_THEN_ID);

        List<String> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : taxed) {
            long nights = ChronoUnit.DAYS.between(reservation.checkIn(), reservation.checkOut());
            BigDecimal tax = taxPerGuestNight.multiply(BigDecimal.valueOf(reservation.guests() * nights))
                    .setScale(2, RoundingMode.HALF_EVEN);
            rows.add(reservation.id() + "," + reservation.roomNumber() + "," + reservation.checkIn() + ","
                    + reservation.checkOut() + "," + reservation.guests() + "," + nights + ","
                    + tax.toPlainString());
            total = total.add(tax);
        }
        return new Content(rows, "total," + total.toPlainString());
    }
}

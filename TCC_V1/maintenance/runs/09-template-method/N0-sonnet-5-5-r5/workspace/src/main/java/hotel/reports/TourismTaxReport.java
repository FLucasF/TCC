package hotel.reports;

import hotel.reservations.PriceCalculator;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** The city's tourism tax, per guest per night, on the paid reservations that start in a month (CSV). */
public final class TourismTaxReport extends CsvReport<YearMonth> {

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
    protected Body body(YearMonth month) {
        List<String> rows = new ArrayList<>();
        BigDecimal total = PriceCalculator.cents(BigDecimal.ZERO);
        for (Reservation reservation : store.paidStartingIn(month)) {
            int nights = reservation.price().nights();
            BigDecimal tax = PriceCalculator.cents(
                    taxPerGuestNight.multiply(BigDecimal.valueOf((long) reservation.guests() * nights)));
            rows.add(line(reservation.id(), reservation.roomNumber(), reservation.checkIn(),
                    reservation.checkOut(), reservation.guests(), nights, tax.toPlainString()));
            total = total.add(tax);
        }
        return new Body(rows, "total," + total.toPlainString());
    }
}

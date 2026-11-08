package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The tourism tax the city charges on the paid reservations that start in a given month (CSV): a fixed
 * amount per guest per night.
 */
public final class TourismTaxReport extends Report<YearMonth, Reservation> {

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
    protected List<Reservation> items(YearMonth month) {
        List<Reservation> taxed = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            if (reservation.isPaid() && YearMonth.from(reservation.checkIn()).equals(month)) {
                taxed.add(reservation);
            }
        }
        taxed.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));
        return taxed;
    }

    @Override
    protected String line(Reservation reservation) {
        return reservation.id() + "," + reservation.roomNumber() + "," + reservation.checkIn() + ","
                + reservation.checkOut() + "," + reservation.guests() + "," + reservation.price().nights() + ","
                + tax(reservation).toPlainString();
    }

    @Override
    protected String totalLine(List<Reservation> taxed) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : taxed) {
            total = total.add(tax(reservation));
        }
        return "total," + total.toPlainString();
    }

    private BigDecimal tax(Reservation reservation) {
        return taxPerGuestNight
                .multiply(BigDecimal.valueOf((long) reservation.guests() * reservation.price().nights()))
                .setScale(2, RoundingMode.HALF_EVEN);
    }
}

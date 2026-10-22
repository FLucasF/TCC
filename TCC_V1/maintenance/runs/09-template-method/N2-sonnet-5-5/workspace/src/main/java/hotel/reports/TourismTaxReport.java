package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The tourism tax the city charges on the paid reservations that start in a given month (CSV). */
public final class TourismTaxReport extends Report<YearMonth, Reservation> {

    private final ReservationStore store;
    private final BigDecimal taxPerGuestPerNight;

    public TourismTaxReport(ReservationStore store, BigDecimal taxPerGuestPerNight) {
        this.store = store;
        this.taxPerGuestPerNight = taxPerGuestPerNight;
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
    protected List<Reservation> rows(YearMonth month) {
        List<Reservation> paid = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            if (reservation.isPaid() && YearMonth.from(reservation.checkIn()).equals(month)) {
                paid.add(reservation);
            }
        }
        paid.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));
        return paid;
    }

    @Override
    protected String line(Reservation reservation) {
        return reservation.id() + "," + reservation.roomNumber() + "," + reservation.checkIn() + ","
                + reservation.checkOut() + "," + reservation.guests() + "," + nights(reservation) + ","
                + tax(reservation).toPlainString();
    }

    @Override
    protected String total(List<Reservation> rows) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : rows) {
            total = total.add(tax(reservation));
        }
        return "total," + total.toPlainString();
    }

    private static long nights(Reservation reservation) {
        return ChronoUnit.DAYS.between(reservation.checkIn(), reservation.checkOut());
    }

    private BigDecimal tax(Reservation reservation) {
        return taxPerGuestPerNight
                .multiply(BigDecimal.valueOf(reservation.guests() * nights(reservation)))
                .setScale(2, RoundingMode.HALF_EVEN);
    }
}

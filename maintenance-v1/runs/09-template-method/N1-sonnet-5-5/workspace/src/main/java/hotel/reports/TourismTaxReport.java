package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The tourism tax the city charges, per guest per night, on the paid reservations that start in a month (CSV). */
public final class TourismTaxReport extends CsvReport<YearMonth> {

    private final ReservationStore store;
    private final BigDecimal taxPerGuestNight;

    public TourismTaxReport(ReservationStore store, BigDecimal taxPerGuestNight) {
        super("tourism-tax", PeriodKinds.MONTH);
        this.store = store;
        this.taxPerGuestNight = taxPerGuestNight;
    }

    @Override
    String body(YearMonth month) {
        List<Reservation> taxed = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            if (reservation.isPaid() && YearMonth.from(reservation.checkIn()).equals(month)) {
                taxed.add(reservation);
            }
        }
        taxed.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : taxed) {
            BigDecimal tax = tax(reservation);
            csv.append(reservation.id()).append(',')
                    .append(reservation.roomNumber()).append(',')
                    .append(reservation.checkIn()).append(',')
                    .append(reservation.checkOut()).append(',')
                    .append(reservation.guests()).append(',')
                    .append(reservation.price().nights()).append(',')
                    .append(tax.toPlainString()).append('\n');
            total = total.add(tax);
        }
        csv.append("total,").append(total.toPlainString()).append('\n');
        return csv.toString();
    }

    private BigDecimal tax(Reservation reservation) {
        int guestNights = reservation.guests() * reservation.price().nights();
        return taxPerGuestNight.multiply(BigDecimal.valueOf(guestNights)).setScale(2, RoundingMode.HALF_EVEN);
    }
}

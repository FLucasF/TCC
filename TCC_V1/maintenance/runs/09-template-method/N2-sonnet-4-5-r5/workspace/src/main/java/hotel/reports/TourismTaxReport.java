package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The tourism tax the city charges: R$ 4.50 per guest per night, for paid reservations starting in the month. */
public final class TourismTaxReport extends Report {

    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final ReservationStore store;
    private List<Reservation> taxableReservations;
    private BigDecimal totalTax;

    public TourismTaxReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        this.taxableReservations = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (reservation.isPaid() && startsInMonth) {
                taxableReservations.add(reservation);
            }
        }
        taxableReservations.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));

        this.totalTax = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : taxableReservations) {
            totalTax = totalTax.add(calculateTax(reservation));
        }

        return super.generate(month.toString());
    }

    @Override
    protected String reportName() {
        return "tourism-tax";
    }

    @Override
    protected String header() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected String rows() {
        StringBuilder csv = new StringBuilder();
        for (Reservation reservation : taxableReservations) {
            long nights = ChronoUnit.DAYS.between(reservation.checkIn(), reservation.checkOut());
            BigDecimal tax = calculateTax(reservation);
            csv.append(reservation.id()).append(',')
                    .append(reservation.roomNumber()).append(',')
                    .append(reservation.checkIn()).append(',')
                    .append(reservation.checkOut()).append(',')
                    .append(reservation.guests()).append(',')
                    .append(nights).append(',')
                    .append(tax.toPlainString()).append('\n');
        }
        return csv.toString();
    }

    @Override
    protected String totalLine() {
        return "total," + totalTax.toPlainString();
    }

    private BigDecimal calculateTax(Reservation reservation) {
        long nights = ChronoUnit.DAYS.between(reservation.checkIn(), reservation.checkOut());
        return TAX_PER_GUEST_PER_NIGHT
                .multiply(BigDecimal.valueOf(reservation.guests()))
                .multiply(BigDecimal.valueOf(nights))
                .setScale(2);
    }
}

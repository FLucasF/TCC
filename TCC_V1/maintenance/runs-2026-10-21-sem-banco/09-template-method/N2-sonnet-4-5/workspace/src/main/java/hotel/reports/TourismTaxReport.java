package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The tourism tax the municipality charges, for the accountant (CSV). */
public final class TourismTaxReport extends CsvReport {

    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final ReservationStore store;
    private YearMonth month;
    private List<Reservation> paidReservations;
    private BigDecimal totalTax;

    public TourismTaxReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        this.month = month;
        this.paidReservations = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (reservation.isPaid() && startsInMonth) {
                paidReservations.add(reservation);
            }
        }
        paidReservations.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));

        this.totalTax = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : paidReservations) {
            totalTax = totalTax.add(calculateTax(reservation));
        }

        return generate();
    }

    private BigDecimal calculateTax(Reservation reservation) {
        long nights = ChronoUnit.DAYS.between(reservation.checkIn(), reservation.checkOut());
        return TAX_PER_GUEST_PER_NIGHT.multiply(BigDecimal.valueOf(reservation.guests()))
                .multiply(BigDecimal.valueOf(nights))
                .setScale(2);
    }

    @Override
    protected String reportName() {
        return "tourism-tax";
    }

    @Override
    protected String period() {
        return month.toString();
    }

    @Override
    protected String header() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected String dataRows() {
        StringBuilder rows = new StringBuilder();
        for (Reservation reservation : paidReservations) {
            long nights = ChronoUnit.DAYS.between(reservation.checkIn(), reservation.checkOut());
            BigDecimal tax = calculateTax(reservation);
            rows.append(reservation.id()).append(',')
                    .append(reservation.roomNumber()).append(',')
                    .append(reservation.checkIn()).append(',')
                    .append(reservation.checkOut()).append(',')
                    .append(reservation.guests()).append(',')
                    .append(nights).append(',')
                    .append(tax.toPlainString()).append('\n');
        }
        return rows.toString();
    }

    @Override
    protected String totalRow() {
        return "total," + totalTax.toPlainString();
    }
}

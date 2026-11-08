package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The tourism tax the town hall charges, for the accountant (CSV).
 * The tax is R$ 4.50 per guest per night, on paid reservations that start in the month.
 */
public final class TourismTaxReport extends CsvReport {

    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final ReservationStore store;
    private List<Reservation> paidReservations;
    private BigDecimal totalTax;

    public TourismTaxReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        paidReservations = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (reservation.isPaid() && startsInMonth) {
                paidReservations.add(reservation);
            }
        }
        paidReservations.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));
        return assemble(month.toString());
    }

    @Override
    protected String reportName() {
        return "tourism-tax";
    }

    @Override
    protected String headers() {
        return "reservation,room,check_in,check_out,guests,nights,tax\n";
    }

    @Override
    protected String dataLines() {
        StringBuilder lines = new StringBuilder();
        totalTax = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : paidReservations) {
            long nights = ChronoUnit.DAYS.between(reservation.checkIn(), reservation.checkOut());
            BigDecimal tax = TAX_PER_GUEST_PER_NIGHT
                    .multiply(BigDecimal.valueOf(reservation.guests()))
                    .multiply(BigDecimal.valueOf(nights));
            lines.append(reservation.id()).append(',')
                    .append(reservation.roomNumber()).append(',')
                    .append(reservation.checkIn()).append(',')
                    .append(reservation.checkOut()).append(',')
                    .append(reservation.guests()).append(',')
                    .append(nights).append(',')
                    .append(tax.toPlainString()).append('\n');
            totalTax = totalTax.add(tax);
        }
        return lines.toString();
    }

    @Override
    protected String totalLine() {
        return "total," + totalTax.toPlainString() + '\n';
    }
}

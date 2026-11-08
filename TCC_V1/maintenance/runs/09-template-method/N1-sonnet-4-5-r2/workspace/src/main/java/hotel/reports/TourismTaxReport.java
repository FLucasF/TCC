package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The tourism tax the city charges, for the accountant (CSV). */
public final class TourismTaxReport extends CsvReport {

    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final ReservationStore store;
    private YearMonth month;

    public TourismTaxReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        this.month = month;
        return generate();
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
    protected String headerLine() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected List<String> dataLines() {
        List<Reservation> taxableReservations = taxableReservations();
        List<String> lines = new ArrayList<>();
        for (Reservation reservation : taxableReservations) {
            long nights = ChronoUnit.DAYS.between(reservation.checkIn(), reservation.checkOut());
            BigDecimal tax = TAX_PER_GUEST_PER_NIGHT
                    .multiply(BigDecimal.valueOf(reservation.guests()))
                    .multiply(BigDecimal.valueOf(nights));
            lines.add(reservation.id() + ","
                    + reservation.roomNumber() + ","
                    + reservation.checkIn() + ","
                    + reservation.checkOut() + ","
                    + reservation.guests() + ","
                    + nights + ","
                    + tax.toPlainString());
        }
        return lines;
    }

    @Override
    protected String totalLine() {
        BigDecimal totalTax = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : taxableReservations()) {
            long nights = ChronoUnit.DAYS.between(reservation.checkIn(), reservation.checkOut());
            BigDecimal tax = TAX_PER_GUEST_PER_NIGHT
                    .multiply(BigDecimal.valueOf(reservation.guests()))
                    .multiply(BigDecimal.valueOf(nights));
            totalTax = totalTax.add(tax);
        }
        return "total," + totalTax.toPlainString();
    }

    private List<Reservation> taxableReservations() {
        List<Reservation> taxable = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (reservation.isPaid() && startsInMonth) {
                taxable.add(reservation);
            }
        }
        taxable.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));
        return taxable;
    }
}

package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The tourism tax the city charges for paid reservations that start in a given month (CSV). */
public final class TourismTaxReport extends Report {

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
    protected String header() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected String dataLines() {
        List<Reservation> paidInMonth = paidReservationsStartingInMonth();

        StringBuilder lines = new StringBuilder();
        for (Reservation reservation : paidInMonth) {
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
        }
        return lines.toString();
    }

    @Override
    protected String totalLine() {
        List<Reservation> paidInMonth = paidReservationsStartingInMonth();
        BigDecimal totalTax = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : paidInMonth) {
            long nights = ChronoUnit.DAYS.between(reservation.checkIn(), reservation.checkOut());
            BigDecimal tax = TAX_PER_GUEST_PER_NIGHT
                    .multiply(BigDecimal.valueOf(reservation.guests()))
                    .multiply(BigDecimal.valueOf(nights));
            totalTax = totalTax.add(tax);
        }
        return "total," + totalTax.toPlainString();
    }

    private List<Reservation> paidReservationsStartingInMonth() {
        List<Reservation> paidInMonth = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (reservation.isPaid() && startsInMonth) {
                paidInMonth.add(reservation);
            }
        }
        paidInMonth.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));
        return paidInMonth;
    }
}

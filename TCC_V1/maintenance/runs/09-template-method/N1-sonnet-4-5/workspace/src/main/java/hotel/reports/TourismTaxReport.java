package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The tourism tax the city charges, for the city office (CSV). */
public final class TourismTaxReport {

    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final ReservationStore store;

    public TourismTaxReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        List<Reservation> taxable = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (reservation.isPaid() && startsInMonth) {
                taxable.add(reservation);
            }
        }
        taxable.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");
        BigDecimal totalTax = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : taxable) {
            long nights = ChronoUnit.DAYS.between(reservation.checkIn(), reservation.checkOut());
            BigDecimal tax = TAX_PER_GUEST_PER_NIGHT
                    .multiply(BigDecimal.valueOf(reservation.guests()))
                    .multiply(BigDecimal.valueOf(nights));
            csv.append(reservation.id()).append(',')
                    .append(reservation.roomNumber()).append(',')
                    .append(reservation.checkIn()).append(',')
                    .append(reservation.checkOut()).append(',')
                    .append(reservation.guests()).append(',')
                    .append(nights).append(',')
                    .append(tax.toPlainString()).append('\n');
            totalTax = totalTax.add(tax);
        }
        csv.append("total,").append(totalTax.toPlainString()).append('\n');
        return ReportFormatter.withTitle("tourism-tax", month.toString(), csv.toString());
    }
}

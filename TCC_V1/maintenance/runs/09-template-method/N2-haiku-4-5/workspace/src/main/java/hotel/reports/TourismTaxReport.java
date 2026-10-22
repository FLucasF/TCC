package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Tourism tax report: R$ 4.50 per guest per night on paid reservations starting in the month. */
public final class TourismTaxReport extends Report {

    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final ReservationStore store;
    private YearMonth month;

    public TourismTaxReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        this.month = month;
        return build();
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
    protected String headers() {
        return "reservation,room,check_in,check_out,guests,nights,tax\n";
    }

    @Override
    protected String rows() {
        List<Reservation> relevant = relevantReservations();
        StringBuilder csv = new StringBuilder();
        for (Reservation reservation : relevant) {
            int nights = reservation.price().nights();
            BigDecimal tax = TAX_PER_GUEST_PER_NIGHT
                    .multiply(new BigDecimal(reservation.guests()))
                    .multiply(new BigDecimal(nights));
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
    protected String total() {
        List<Reservation> relevant = relevantReservations();
        BigDecimal totalTax = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : relevant) {
            int nights = reservation.price().nights();
            BigDecimal tax = TAX_PER_GUEST_PER_NIGHT
                    .multiply(new BigDecimal(reservation.guests()))
                    .multiply(new BigDecimal(nights));
            totalTax = totalTax.add(tax);
        }
        return "total," + totalTax.toPlainString() + "\n";
    }

    private List<Reservation> relevantReservations() {
        List<Reservation> relevant = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean isPaid = reservation.isPaid();
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (isPaid && startsInMonth) {
                relevant.add(reservation);
            }
        }
        relevant.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));
        return relevant;
    }
}

package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The tourism tax the city charges: R$ 4.50 per guest per night, from paid reservations that start
 * in a given month (CSV).
 */
public final class TourismTaxReport extends Report {

    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final ReservationStore store;
    private List<Reservation> paidReservations;
    private BigDecimal totalTax;

    public TourismTaxReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        this.paidReservations = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (reservation.isPaid() && startsInMonth) {
                paidReservations.add(reservation);
            }
        }
        paidReservations.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));
        this.totalTax = BigDecimal.ZERO.setScale(2);
        return super.generate(month.toString());
    }

    @Override
    protected String reportName() {
        return "tourism-tax";
    }

    @Override
    protected String header() {
        return "reservation,room,check_in,check_out,guests,nights,tax\n";
    }

    @Override
    protected String dataRows() {
        StringBuilder rows = new StringBuilder();
        for (Reservation reservation : paidReservations) {
            int nights = reservation.price().nights();
            BigDecimal tax = TAX_PER_GUEST_PER_NIGHT
                    .multiply(BigDecimal.valueOf(reservation.guests()))
                    .multiply(BigDecimal.valueOf(nights))
                    .setScale(2);
            rows.append(reservation.id()).append(',')
                    .append(reservation.roomNumber()).append(',')
                    .append(reservation.checkIn()).append(',')
                    .append(reservation.checkOut()).append(',')
                    .append(reservation.guests()).append(',')
                    .append(nights).append(',')
                    .append(tax.toPlainString()).append('\n');
            totalTax = totalTax.add(tax);
        }
        return rows.toString();
    }

    @Override
    protected String totalRow() {
        return "total," + totalTax.toPlainString() + '\n';
    }
}

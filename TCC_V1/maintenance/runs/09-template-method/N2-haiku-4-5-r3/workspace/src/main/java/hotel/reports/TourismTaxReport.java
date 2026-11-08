package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Tourism tax report: R$ 4.50 per guest per night for paid reservations starting in the month. */
public final class TourismTaxReport extends Report<YearMonth> {

    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");
    private final ReservationStore store;

    public TourismTaxReport(ReservationStore store) {
        this.store = store;
    }

    @Override
    protected String reportName() {
        return "tourism-tax";
    }

    @Override
    protected String formatTitle(YearMonth month) {
        return titleLine(month);
    }

    @Override
    protected String formatHeader() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected List<String> formatLines(YearMonth month) {
        List<Reservation> taxable = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean isPaid = reservation.isPaid();
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (isPaid && startsInMonth) {
                taxable.add(reservation);
            }
        }
        taxable.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));

        List<String> lines = new ArrayList<>();
        for (Reservation reservation : taxable) {
            int nights = reservation.price().nights();
            BigDecimal tax = calculateTax(reservation.guests(), nights);
            lines.add(reservation.id() + "," + reservation.roomNumber() + ","
                    + reservation.checkIn() + "," + reservation.checkOut() + ","
                    + reservation.guests() + "," + nights + "," + tax.toPlainString());
        }
        return lines;
    }

    @Override
    protected String formatTotal(YearMonth month) {
        BigDecimal totalTax = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : store.all()) {
            boolean isPaid = reservation.isPaid();
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (isPaid && startsInMonth) {
                int nights = reservation.price().nights();
                totalTax = totalTax.add(calculateTax(reservation.guests(), nights));
            }
        }
        return "total," + totalTax.toPlainString();
    }

    private BigDecimal calculateTax(int guests, int nights) {
        return TAX_PER_GUEST_PER_NIGHT.multiply(new BigDecimal(guests * nights)).setScale(2, java.math.RoundingMode.HALF_EVEN);
    }
}

package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Tourism tax (R$ 4.50 per guest per night) for paid reservations starting in a given month.
 */
public final class TourismTaxReport extends Report<TourismTaxReport.TourismTaxRow> {

    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    record TourismTaxRow(long reservationId, int roomNumber, LocalDate checkIn,
                         LocalDate checkOut, int guests, long nights, BigDecimal tax) {}

    private final ReservationStore store;

    public TourismTaxReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        return generateCsv(month);
    }

    @Override
    protected String reportName() {
        return "tourism-tax";
    }

    @Override
    protected List<TourismTaxRow> collectData(Object period) {
        YearMonth month = (YearMonth) period;
        List<Reservation> paidReservations = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (reservation.isPaid() && startsInMonth) {
                paidReservations.add(reservation);
            }
        }
        paidReservations.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));

        List<TourismTaxRow> rows = new ArrayList<>();
        for (Reservation reservation : paidReservations) {
            long nights = ChronoUnit.DAYS.between(reservation.checkIn(), reservation.checkOut());
            BigDecimal tax = TAX_PER_GUEST_PER_NIGHT
                    .multiply(BigDecimal.valueOf(reservation.guests()))
                    .multiply(BigDecimal.valueOf(nights));
            rows.add(new TourismTaxRow(reservation.id(), reservation.roomNumber(),
                    reservation.checkIn(), reservation.checkOut(),
                    reservation.guests(), nights, tax));
        }
        return rows;
    }

    @Override
    protected String headerLine() {
        return "reservation,room,check_in,check_out,guests,nights,tax\n";
    }

    @Override
    protected String dataLine(TourismTaxRow row) {
        return row.reservationId + "," + row.roomNumber + "," + row.checkIn + ","
                + row.checkOut + "," + row.guests + "," + row.nights + ","
                + row.tax.toPlainString() + "\n";
    }

    @Override
    protected String totalLine(List<TourismTaxRow> data) {
        BigDecimal totalTax = BigDecimal.ZERO;
        for (TourismTaxRow row : data) {
            totalTax = totalTax.add(row.tax);
        }
        return "total," + totalTax.toPlainString() + "\n";
    }
}

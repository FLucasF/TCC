package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Tourism tax per guest per night, for the city council (CSV). */
public final class TourismTaxReport extends Report {

    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final ReservationStore store;
    private final YearMonth month;
    private BigDecimal totalTax;

    public TourismTaxReport(ReservationStore store) {
        this.store = store;
        this.month = null;
    }

    private TourismTaxReport(ReservationStore store, YearMonth month) {
        this.store = store;
        this.month = month;
    }

    public String generate(YearMonth month) {
        return new TourismTaxReport(store, month).generate();
    }

    @Override
    protected String title() {
        return POUSADA_NAME + ",tourism-tax," + month;
    }

    @Override
    protected String header() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected void data(List<String> lines) {
        List<Reservation> paidReservations = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (reservation.isPaid() && startsInMonth) {
                paidReservations.add(reservation);
            }
        }
        paidReservations.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));

        totalTax = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : paidReservations) {
            int nights = reservation.price().nights();
            int guests = reservation.guests();
            BigDecimal tax = TAX_PER_GUEST_PER_NIGHT
                    .multiply(BigDecimal.valueOf(guests * nights))
                    .setScale(2, RoundingMode.HALF_UP);
            lines.add(reservation.id() + "," + reservation.roomNumber() + "," + reservation.checkIn() + ","
                    + reservation.checkOut() + "," + guests + "," + nights + "," + tax.toPlainString());
            totalTax = totalTax.add(tax);
        }
    }

    @Override
    protected String total() {
        return "total," + totalTax.toPlainString();
    }
}

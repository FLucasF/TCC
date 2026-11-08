package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** The city's tourism tax on the paid reservations that start in a month: a fee per guest per night (CSV). */
public final class TourismTaxReport extends MonthlyReport {

    private final ReservationStore store;
    private final BigDecimal feePerGuestNight;

    public TourismTaxReport(ReservationStore store, BigDecimal feePerGuestNight) {
        this.store = store;
        this.feePerGuestNight = feePerGuestNight;
    }

    @Override
    public String name() {
        return "tourism-tax";
    }

    @Override
    protected Table table(YearMonth month) {
        List<String> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : MonthReservations.paid(store, month)) {
            int nights = reservation.price().nights();
            BigDecimal tax = feePerGuestNight.multiply(BigDecimal.valueOf((long) reservation.guests() * nights))
                    .setScale(2, RoundingMode.HALF_EVEN);
            rows.add(line(reservation.id(), reservation.roomNumber(), reservation.checkIn(),
                    reservation.checkOut(), reservation.guests(), nights, tax.toPlainString()));
            total = total.add(tax);
        }
        return new Table("reservation,room,check_in,check_out,guests,nights,tax", rows,
                line("total", total.toPlainString()));
    }
}

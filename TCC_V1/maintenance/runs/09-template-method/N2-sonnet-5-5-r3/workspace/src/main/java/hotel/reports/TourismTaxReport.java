package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** The tourism tax the city charges, per guest per night, on the paid reservations starting in a month (CSV). */
public final class TourismTaxReport extends Report<YearMonth> {

    private final ReservationStore store;
    private final BigDecimal taxPerGuestNight;

    public TourismTaxReport(ReservationStore store, BigDecimal taxPerGuestNight) {
        this.store = store;
        this.taxPerGuestNight = taxPerGuestNight;
    }

    @Override
    public String name() {
        return "tourism-tax";
    }

    @Override
    protected String header() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected List<String> lines(YearMonth month) {
        List<String> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : MonthReservations.paid(store, month)) {
            int nights = reservation.price().nights();
            BigDecimal tax = taxPerGuestNight.multiply(BigDecimal.valueOf((long) reservation.guests() * nights))
                    .setScale(2, RoundingMode.HALF_EVEN);
            lines.add(reservation.id() + "," + reservation.roomNumber() + "," + reservation.checkIn() + ","
                    + reservation.checkOut() + "," + reservation.guests() + "," + nights + ","
                    + tax.toPlainString());
            total = total.add(tax);
        }
        lines.add("total," + total.toPlainString());
        return lines;
    }
}

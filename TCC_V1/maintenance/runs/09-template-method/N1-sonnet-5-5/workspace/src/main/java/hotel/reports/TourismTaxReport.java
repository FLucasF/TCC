package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** The city's tourism tax on the paid reservations that start in a given month: a rate per guest per night (CSV). */
public final class TourismTaxReport extends CsvReport<YearMonth> {

    private final ReservationStore store;
    private final BigDecimal ratePerGuestNight;

    public TourismTaxReport(ReservationStore store, BigDecimal ratePerGuestNight) {
        super("tourism-tax");
        this.store = store;
        this.ratePerGuestNight = ratePerGuestNight;
    }

    @Override
    String header() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    List<String> lines(YearMonth month) {
        List<String> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : MonthReservations.paid(store, month)) {
            int nights = reservation.price().nights();
            BigDecimal tax = ratePerGuestNight.multiply(BigDecimal.valueOf((long) reservation.guests() * nights))
                    .setScale(2);
            lines.add(reservation.id() + "," + reservation.roomNumber() + "," + reservation.checkIn() + ","
                    + reservation.checkOut() + "," + reservation.guests() + "," + nights + ","
                    + tax.toPlainString());
            total = total.add(tax);
        }
        lines.add("total," + total.toPlainString());
        return lines;
    }
}

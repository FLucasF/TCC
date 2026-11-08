package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** The tourism tax the city charges on the paid reservations that start in a month: a rate per guest per night. */
public final class TourismTaxReport extends CsvReport<YearMonth> {

    private final ReservationStore store;
    private final BigDecimal ratePerGuestNight;

    public TourismTaxReport(ReservationStore store, BigDecimal ratePerGuestNight) {
        this.store = store;
        this.ratePerGuestNight = ratePerGuestNight;
    }

    @Override
    public String name() {
        return "tourism-tax";
    }

    @Override
    public String periodParam() {
        return "month";
    }

    @Override
    public YearMonth parsePeriod(String text) {
        return parseMonth(text);
    }

    @Override
    protected List<String> lines(YearMonth month) {
        List<String> lines = new ArrayList<>();
        lines.add("reservation,room,check_in,check_out,guests,nights,tax");
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : MonthlyReservations.startingIn(store, month, Reservation::isPaid)) {
            long nights = ChronoUnit.DAYS.between(reservation.checkIn(), reservation.checkOut());
            BigDecimal tax = ratePerGuestNight
                    .multiply(BigDecimal.valueOf(reservation.guests()))
                    .multiply(BigDecimal.valueOf(nights))
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

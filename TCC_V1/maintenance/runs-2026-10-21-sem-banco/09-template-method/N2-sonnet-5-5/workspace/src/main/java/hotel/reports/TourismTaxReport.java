package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** The tourism tax the city charges per guest per night, on the paid reservations that start in a month (CSV). */
public final class TourismTaxReport extends Report<YearMonth, Reservation> {

    private final ReservationStore store;
    private final BigDecimal ratePerGuestNight;

    public TourismTaxReport(ReservationStore store, BigDecimal ratePerGuestNight) {
        this.store = store;
        this.ratePerGuestNight = ratePerGuestNight;
    }

    @Override
    protected String name() {
        return "tourism-tax";
    }

    @Override
    protected String header() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected List<Reservation> rows(YearMonth month) {
        return MonthlyReservations.startingIn(store, month, Reservation::isPaid);
    }

    @Override
    protected String line(Reservation reservation) {
        return reservation.id() + "," + reservation.roomNumber() + ","
                + reservation.checkIn() + "," + reservation.checkOut() + ","
                + reservation.guests() + "," + reservation.price().nights() + ","
                + tax(reservation).toPlainString();
    }

    @Override
    protected String total(List<Reservation> reservations) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : reservations) {
            total = total.add(tax(reservation));
        }
        return "total," + total.toPlainString();
    }

    private BigDecimal tax(Reservation reservation) {
        return ratePerGuestNight
                .multiply(BigDecimal.valueOf((long) reservation.guests() * reservation.price().nights()))
                .setScale(2, java.math.RoundingMode.HALF_EVEN);
    }
}

package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** The tourism tax the city charges per guest per night, on the paid reservations that start in a month (CSV). */
public final class TourismTaxReport extends CsvReport<YearMonth> {

    private final ReservationStore store;
    private final BigDecimal taxPerGuestPerNight;

    public TourismTaxReport(ReservationStore store, BigDecimal taxPerGuestPerNight) {
        this.store = store;
        this.taxPerGuestPerNight = taxPerGuestPerNight;
    }

    @Override
    public String name() {
        return "tourism-tax";
    }

    @Override
    protected Table table(YearMonth month) {
        List<Reservation> paid = MonthlyReservations.startingIn(store, month, Reservation::isPaid);
        List<String> rows = paid.stream()
                .map(reservation -> reservation.id() + ","
                        + reservation.roomNumber() + ","
                        + reservation.checkIn() + ","
                        + reservation.checkOut() + ","
                        + reservation.guests() + ","
                        + reservation.price().nights() + ","
                        + taxOf(reservation).toPlainString())
                .toList();
        BigDecimal total = paid.stream()
                .map(this::taxOf)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        return new Table("reservation,room,check_in,check_out,guests,nights,tax", rows, "total," + total.toPlainString());
    }

    private BigDecimal taxOf(Reservation reservation) {
        return taxPerGuestPerNight
                .multiply(BigDecimal.valueOf((long) reservation.guests() * reservation.price().nights()))
                .setScale(2);
    }
}

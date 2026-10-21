package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * The money the inn kept from the reservations that start in a given month, for the accountant (CSV).
 * A cancelled non-refundable reservation counts: the guest paid and nothing went back.
 */
public final class RevenueReport extends CsvReport<YearMonth> {

    private final ReservationStore store;

    public RevenueReport(ReservationStore store) {
        this.store = store;
    }

    @Override
    public String name() {
        return "revenue";
    }

    @Override
    protected Table table(YearMonth month) {
        List<Reservation> withRevenue = MonthlyReservations.withRevenueStartingIn(store, month);
        List<String> rows = withRevenue.stream()
                .map(reservation -> reservation.id() + ","
                        + reservation.roomNumber() + ","
                        + reservation.checkIn() + ","
                        + reservation.checkOut() + ","
                        + reservation.status() + ","
                        + reservation.amountKept().toPlainString())
                .toList();
        BigDecimal revenue = withRevenue.stream()
                .map(Reservation::amountKept)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        return new Table("reservation,room,check_in,check_out,status,amount", rows, "total," + revenue.toPlainString());
    }
}

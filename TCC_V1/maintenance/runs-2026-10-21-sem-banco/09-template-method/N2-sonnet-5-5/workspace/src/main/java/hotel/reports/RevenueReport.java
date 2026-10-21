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
public final class RevenueReport extends Report<YearMonth, Reservation> {

    private final ReservationStore store;

    public RevenueReport(ReservationStore store) {
        this.store = store;
    }

    @Override
    protected String name() {
        return "revenue";
    }

    @Override
    protected String header() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    protected List<Reservation> rows(YearMonth month) {
        return MonthlyReservations.withRevenue(store, month);
    }

    @Override
    protected String line(Reservation reservation) {
        return reservation.id() + "," + reservation.roomNumber() + ","
                + reservation.checkIn() + "," + reservation.checkOut() + ","
                + reservation.status() + "," + reservation.amountKept().toPlainString();
    }

    @Override
    protected String total(List<Reservation> withRevenue) {
        BigDecimal revenue = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : withRevenue) {
            revenue = revenue.add(reservation.amountKept());
        }
        return "total," + revenue.toPlainString();
    }
}

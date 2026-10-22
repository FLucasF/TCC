package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
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

    /** The reservations that start in the month and left money with the inn, in the order of this report. */
    static List<Reservation> withRevenue(ReservationStore store, YearMonth month) {
        List<Reservation> withRevenue = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                withRevenue.add(reservation);
            }
        }
        withRevenue.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));
        return withRevenue;
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
        return withRevenue(store, month);
    }

    @Override
    protected String line(Reservation reservation) {
        return reservation.id() + "," + reservation.roomNumber() + "," + reservation.checkIn() + ","
                + reservation.checkOut() + "," + reservation.status() + ","
                + reservation.amountKept().toPlainString();
    }

    @Override
    protected String total(List<Reservation> rows) {
        BigDecimal revenue = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : rows) {
            revenue = revenue.add(reservation.amountKept());
        }
        return "total," + revenue.toPlainString();
    }
}

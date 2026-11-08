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
public final class RevenueReport extends Report {

    private final ReservationStore store;
    private final YearMonth month;
    private BigDecimal revenue;

    public RevenueReport(ReservationStore store) {
        this.store = store;
        this.month = null;
    }

    private RevenueReport(ReservationStore store, YearMonth month) {
        this.store = store;
        this.month = month;
    }

    public String generate(YearMonth month) {
        return new RevenueReport(store, month).generate();
    }

    @Override
    protected String title() {
        return POUSADA_NAME + ",revenue," + month;
    }

    @Override
    protected String header() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    protected void data(List<String> lines) {
        List<Reservation> withRevenue = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                withRevenue.add(reservation);
            }
        }
        withRevenue.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));

        revenue = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : withRevenue) {
            lines.add(reservation.id() + "," + reservation.roomNumber() + "," + reservation.checkIn() + ","
                    + reservation.checkOut() + "," + reservation.status() + ","
                    + reservation.amountKept().toPlainString());
            revenue = revenue.add(reservation.amountKept());
        }
    }

    @Override
    protected String total() {
        return "total," + revenue.toPlainString();
    }
}

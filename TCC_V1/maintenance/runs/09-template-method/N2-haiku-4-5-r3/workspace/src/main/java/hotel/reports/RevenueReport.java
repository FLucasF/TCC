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
public final class RevenueReport extends Report<YearMonth> {

    private final ReservationStore store;

    public RevenueReport(ReservationStore store) {
        this.store = store;
    }

    @Override
    protected String reportName() {
        return "revenue";
    }

    @Override
    protected String formatTitle(YearMonth month) {
        return titleLine(month);
    }

    @Override
    protected String formatHeader() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    protected List<String> formatLines(YearMonth month) {
        List<Reservation> withRevenue = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                withRevenue.add(reservation);
            }
        }
        withRevenue.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));

        List<String> lines = new ArrayList<>();
        for (Reservation reservation : withRevenue) {
            lines.add(reservation.id() + "," + reservation.roomNumber() + ","
                    + reservation.checkIn() + "," + reservation.checkOut() + ","
                    + reservation.status() + "," + reservation.amountKept().toPlainString());
        }
        return lines;
    }

    @Override
    protected String formatTotal(YearMonth month) {
        BigDecimal revenue = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                revenue = revenue.add(reservation.amountKept());
            }
        }
        return "total," + revenue.toPlainString();
    }
}

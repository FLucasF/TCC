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
public final class RevenueReport extends CsvReport {

    private final ReservationStore store;
    private YearMonth month;
    private List<Reservation> withRevenue;
    private BigDecimal revenue;

    public RevenueReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        this.month = month;
        this.withRevenue = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                withRevenue.add(reservation);
            }
        }
        withRevenue.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));

        this.revenue = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : withRevenue) {
            revenue = revenue.add(reservation.amountKept());
        }

        return generate();
    }

    @Override
    protected String reportName() {
        return "revenue";
    }

    @Override
    protected String period() {
        return month.toString();
    }

    @Override
    protected String header() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    protected String dataRows() {
        StringBuilder rows = new StringBuilder();
        for (Reservation reservation : withRevenue) {
            rows.append(reservation.id()).append(',')
                    .append(reservation.roomNumber()).append(',')
                    .append(reservation.checkIn()).append(',')
                    .append(reservation.checkOut()).append(',')
                    .append(reservation.status()).append(',')
                    .append(reservation.amountKept().toPlainString()).append('\n');
        }
        return rows.toString();
    }

    @Override
    protected String totalRow() {
        return "total," + revenue.toPlainString();
    }
}

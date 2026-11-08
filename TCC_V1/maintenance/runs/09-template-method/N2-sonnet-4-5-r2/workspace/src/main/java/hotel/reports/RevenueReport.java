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
public final class RevenueReport extends ReportTemplate<YearMonth> {

    private final ReservationStore store;

    public RevenueReport(ReservationStore store) {
        this.store = store;
    }

    @Override
    protected String reportName() {
        return "revenue";
    }

    @Override
    protected String headerLine() {
        return "reservation,room,check_in,check_out,status,amount\n";
    }

    @Override
    protected String dataLines(YearMonth month) {
        List<Reservation> withRevenue = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                withRevenue.add(reservation);
            }
        }
        withRevenue.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));

        StringBuilder lines = new StringBuilder();
        BigDecimal revenue = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : withRevenue) {
            lines.append(reservation.id()).append(',')
                    .append(reservation.roomNumber()).append(',')
                    .append(reservation.checkIn()).append(',')
                    .append(reservation.checkOut()).append(',')
                    .append(reservation.status()).append(',')
                    .append(reservation.amountKept().toPlainString()).append('\n');
            revenue = revenue.add(reservation.amountKept());
        }
        this.totalRevenue = revenue;
        return lines.toString();
    }

    @Override
    protected String totalLine(YearMonth month) {
        return "total," + totalRevenue.toPlainString() + '\n';
    }

    private BigDecimal totalRevenue;
}

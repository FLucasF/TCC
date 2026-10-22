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
    private YearMonth month;

    public RevenueReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        this.month = month;
        return build();
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
    protected String headers() {
        return "reservation,room,check_in,check_out,status,amount\n";
    }

    @Override
    protected String rows() {
        List<Reservation> withRevenue = relevantReservations();
        StringBuilder csv = new StringBuilder();
        for (Reservation reservation : withRevenue) {
            csv.append(reservation.id()).append(',')
                    .append(reservation.roomNumber()).append(',')
                    .append(reservation.checkIn()).append(',')
                    .append(reservation.checkOut()).append(',')
                    .append(reservation.status()).append(',')
                    .append(reservation.amountKept().toPlainString()).append('\n');
        }
        return csv.toString();
    }

    @Override
    protected String total() {
        List<Reservation> withRevenue = relevantReservations();
        BigDecimal revenue = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : withRevenue) {
            revenue = revenue.add(reservation.amountKept());
        }
        return "total," + revenue.toPlainString() + "\n";
    }

    private List<Reservation> relevantReservations() {
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
}

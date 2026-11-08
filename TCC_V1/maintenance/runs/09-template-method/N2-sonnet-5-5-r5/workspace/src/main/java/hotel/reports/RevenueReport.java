package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
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
    protected String name() {
        return "revenue";
    }

    @Override
    protected List<String> header() {
        return List.of("reservation", "room", "check_in", "check_out", "status", "amount");
    }

    @Override
    protected Table table(YearMonth month) {
        List<List<String>> lines = new ArrayList<>();
        BigDecimal revenue = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : MonthlyRevenue.of(store, month)) {
            lines.add(List.of(
                    String.valueOf(reservation.id()),
                    String.valueOf(reservation.roomNumber()),
                    reservation.checkIn().toString(),
                    reservation.checkOut().toString(),
                    reservation.status().toString(),
                    reservation.amountKept().toPlainString()));
            revenue = revenue.add(reservation.amountKept());
        }
        return new Table(lines, List.of("total", revenue.toPlainString()));
    }
}

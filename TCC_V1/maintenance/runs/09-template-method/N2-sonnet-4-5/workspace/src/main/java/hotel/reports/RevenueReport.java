package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import hotel.reservations.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The money the inn kept from the reservations that start in a given month, for the accountant (CSV).
 * A cancelled non-refundable reservation counts: the guest paid and nothing went back.
 */
public final class RevenueReport extends Report<RevenueReport.RevenueRow> {

    record RevenueRow(long reservationId, int roomNumber, LocalDate checkIn,
                      LocalDate checkOut, ReservationStatus status, BigDecimal amount) {}

    private final ReservationStore store;

    public RevenueReport(ReservationStore store) {
        this.store = store;
    }

    public String generate(YearMonth month) {
        return generateCsv(month);
    }

    @Override
    protected String reportName() {
        return "revenue";
    }

    @Override
    protected List<RevenueRow> collectData(Object period) {
        YearMonth month = (YearMonth) period;
        List<Reservation> withRevenue = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            boolean startsInMonth = YearMonth.from(reservation.checkIn()).equals(month);
            if (keptMoney && startsInMonth) {
                withRevenue.add(reservation);
            }
        }
        withRevenue.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));

        List<RevenueRow> rows = new ArrayList<>();
        for (Reservation reservation : withRevenue) {
            rows.add(new RevenueRow(reservation.id(), reservation.roomNumber(),
                    reservation.checkIn(), reservation.checkOut(),
                    reservation.status(), reservation.amountKept()));
        }
        return rows;
    }

    @Override
    protected String headerLine() {
        return "reservation,room,check_in,check_out,status,amount\n";
    }

    @Override
    protected String dataLine(RevenueRow row) {
        return row.reservationId + "," + row.roomNumber + "," + row.checkIn + ","
                + row.checkOut + "," + row.status + "," + row.amount.toPlainString() + "\n";
    }

    @Override
    protected String totalLine(List<RevenueRow> data) {
        BigDecimal revenue = BigDecimal.ZERO.setScale(2);
        for (RevenueRow row : data) {
            revenue = revenue.add(row.amount);
        }
        return "total," + revenue.toPlainString() + "\n";
    }
}

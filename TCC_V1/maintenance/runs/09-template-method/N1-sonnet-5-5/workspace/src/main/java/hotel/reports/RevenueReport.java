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
public final class RevenueReport extends CsvReport<YearMonth> {

    private final ReservationStore store;

    public RevenueReport(ReservationStore store) {
        super("revenue");
        this.store = store;
    }

    @Override
    String header() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    List<String> lines(YearMonth month) {
        List<String> lines = new ArrayList<>();
        BigDecimal revenue = BigDecimal.ZERO.setScale(2);
        for (Reservation reservation : MonthReservations.withRevenue(store, month)) {
            lines.add(reservation.id() + "," + reservation.roomNumber() + "," + reservation.checkIn() + ","
                    + reservation.checkOut() + "," + reservation.status() + ","
                    + reservation.amountKept().toPlainString());
            revenue = revenue.add(reservation.amountKept());
        }
        lines.add("total," + revenue.toPlainString());
        return lines;
    }
}

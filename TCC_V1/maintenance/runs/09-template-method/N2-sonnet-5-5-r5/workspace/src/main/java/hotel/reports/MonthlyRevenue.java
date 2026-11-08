package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The reservations that start in a month and left money with the inn, in the order of the financial report. */
final class MonthlyRevenue {

    private MonthlyRevenue() {}

    static List<Reservation> of(ReservationStore store, YearMonth month) {
        List<Reservation> withRevenue = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            boolean keptMoney = reservation.amountKept().signum() > 0;
            if (keptMoney && startsIn(reservation, month)) {
                withRevenue.add(reservation);
            }
        }
        withRevenue.sort(ORDER);
        return withRevenue;
    }

    static final Comparator<Reservation> ORDER =
            Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id);

    static boolean startsIn(Reservation reservation, YearMonth month) {
        return YearMonth.from(reservation.checkIn()).equals(month);
    }
}

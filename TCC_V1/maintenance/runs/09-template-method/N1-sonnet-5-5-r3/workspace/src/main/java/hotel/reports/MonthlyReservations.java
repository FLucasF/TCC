package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/** The reservations that start in a month, in the order the monthly reports list them. */
final class MonthlyReservations {

    private final ReservationStore store;

    MonthlyReservations(ReservationStore store) {
        this.store = store;
    }

    /** Those that start in the month and the inn kept money from: the financial report's reservations. */
    List<Reservation> withRevenue(YearMonth month) {
        return startingIn(month, reservation -> reservation.amountKept().signum() > 0);
    }

    /** Those that start in the month and are paid (confirmed, checked in or checked out). */
    List<Reservation> paid(YearMonth month) {
        return startingIn(month, Reservation::isPaid);
    }

    private List<Reservation> startingIn(YearMonth month, Predicate<Reservation> wanted) {
        return store.all().stream()
                .filter(wanted)
                .filter(reservation -> YearMonth.from(reservation.checkIn()).equals(month))
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id))
                .toList();
    }
}

package hotel.reports;

import hotel.reservations.Reservation;
import hotel.reservations.ReservationStore;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/** The reservations that start in a month, in the order the monthly reports list them. */
final class MonthReservations {

    private MonthReservations() {
    }

    /** Those the inn kept money from; a cancelled non-refundable reservation counts. */
    static List<Reservation> withRevenue(ReservationStore store, YearMonth month) {
        return startingIn(store, month, reservation -> reservation.amountKept().signum() > 0);
    }

    /** Those the guest paid and did not cancel: confirmed, checked in or checked out. */
    static List<Reservation> paid(ReservationStore store, YearMonth month) {
        return startingIn(store, month, Reservation::isPaid);
    }

    private static List<Reservation> startingIn(
            ReservationStore store, YearMonth month, Predicate<Reservation> condition) {
        List<Reservation> found = new ArrayList<>();
        for (Reservation reservation : store.all()) {
            if (condition.test(reservation) && YearMonth.from(reservation.checkIn()).equals(month)) {
                found.add(reservation);
            }
        }
        found.sort(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id));
        return found;
    }
}

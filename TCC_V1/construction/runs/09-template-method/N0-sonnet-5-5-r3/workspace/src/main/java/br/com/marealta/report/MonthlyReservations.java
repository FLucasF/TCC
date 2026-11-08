package br.com.marealta.report;

import br.com.marealta.db.ReservationRepository;
import br.com.marealta.domain.Reservation;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;

/** Shared selections of reservations starting in a month. */
final class MonthlyReservations {

    private MonthlyReservations() {}

    /** Reservations starting in the month, by check-in date then id. */
    static List<Reservation> starting(ReservationRepository repo, Connection c, String month) throws SQLException {
        YearMonth ym = PeriodKind.month(month);
        return repo.findCheckInBetween(c, ym.atDay(1), ym.plusMonths(1).atDay(1));
    }

    /** Reservations starting in the month that left money with the inn (paid minus refunded). */
    static List<Reservation> withRevenue(ReservationRepository repo, Connection c, String month) throws SQLException {
        return starting(repo, c, month).stream().filter(r -> r.amountKept().signum() > 0).toList();
    }
}

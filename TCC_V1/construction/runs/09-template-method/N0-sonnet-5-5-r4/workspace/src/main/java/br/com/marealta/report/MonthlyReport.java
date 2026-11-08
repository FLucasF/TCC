package br.com.marealta.report;

import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.PousadaException;
import br.com.marealta.domain.Reservation;
import br.com.marealta.store.Tx;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

/** A report over the reservations that start in a month. */
abstract class MonthlyReport implements Report {
    @Override
    public final String parameter() {
        return "month";
    }

    @Override
    public final ReportTable generate(String period, Tx tx) {
        YearMonth month;
        try {
            month = YearMonth.parse(period);
        } catch (DateTimeParseException e) {
            throw new PousadaException(ErrorCode.INVALID_REQUEST, "month must be formatted as yyyy-MM");
        }
        return build(month, tx);
    }

    protected abstract ReportTable build(YearMonth month, Tx tx);

    /** Reservations starting in the month, by check-in date and then number. */
    protected static List<Reservation> startingIn(YearMonth month, Tx tx) {
        return tx.reservationsCheckingInBetween(month.atDay(1), month.plusMonths(1).atDay(1)).stream()
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id))
                .toList();
    }

    /** Starting in the month and currently in a paid state (confirmed, checked in, checked out). */
    protected static List<Reservation> paidStartingIn(YearMonth month, Tx tx) {
        return startingIn(month, tx).stream().filter(Reservation::paid).toList();
    }

    /** Starting in the month and leaving money with the inn (paid minus refunded is positive). */
    protected static List<Reservation> retainedStartingIn(YearMonth month, Tx tx) {
        return startingIn(month, tx).stream().filter(r -> r.retained().signum() > 0).toList();
    }
}

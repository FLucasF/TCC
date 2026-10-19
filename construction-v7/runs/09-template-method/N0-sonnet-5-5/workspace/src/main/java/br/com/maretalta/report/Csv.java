package br.com.maretalta.report;

import br.com.maretalta.ApiException;
import br.com.maretalta.domain.Reservation;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

/** Shared helpers for building the reports: layout, period parsing and reservation selection. */
final class Csv {
    static final String INN = "Pousada Maré Alta";

    private Csv() {}

    /** Title line, header, body rows and the closing total line, each terminated by a newline. */
    static String build(String reportName, String period, String header, List<String> rows, String total) {
        StringBuilder sb = new StringBuilder();
        sb.append(INN).append(',').append(reportName).append(',').append(period).append('\n');
        sb.append(header).append('\n');
        rows.forEach(r -> sb.append(r).append('\n'));
        sb.append(total).append('\n');
        return sb.toString();
    }

    static LocalDate parseDate(String raw) {
        if (raw == null) throw ApiException.invalid("date is required (yyyy-MM-dd)");
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("date must be in the format yyyy-MM-dd");
        }
    }

    static YearMonth parseMonth(String raw) {
        if (raw == null) throw ApiException.invalid("month is required (yyyy-MM)");
        if (!raw.matches("\\d{4}-\\d{2}")) throw ApiException.invalid("month must be in the format yyyy-MM");
        try {
            return YearMonth.parse(raw);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("month must be in the format yyyy-MM");
        }
    }

    static final Comparator<Reservation> BY_CHECK_IN_THEN_ID =
            Comparator.<Reservation, LocalDate>comparing(r -> r.checkIn).thenComparingInt(r -> r.id);

    static List<Reservation> startingIn(List<Reservation> all, YearMonth month) {
        return all.stream()
                .filter(r -> YearMonth.from(r.checkIn).equals(month))
                .sorted(BY_CHECK_IN_THEN_ID)
                .toList();
    }

    /** Reservations starting in the month that left money with the inn (paid minus refunded > 0). */
    static List<Reservation> revenueReservations(List<Reservation> all, YearMonth month) {
        return startingIn(all, month).stream().filter(r -> r.netAmount().signum() > 0).toList();
    }
}

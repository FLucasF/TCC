package br.com.marealta.reports;

import br.com.marealta.ApiException;
import br.com.marealta.Reservation;
import br.com.marealta.ReservationService;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/** Relatório de reservas que começam em um mês. */
public abstract class MonthlyReport<T> extends Report<YearMonth, T> {
    protected static final Comparator<Reservation> BY_CHECK_IN_THEN_ID =
            Comparator.comparing(Reservation::getCheckIn).thenComparing(Reservation::getId);

    protected final ReservationService reservations;

    protected MonthlyReport(ReservationService reservations) {
        this.reservations = reservations;
    }

    @Override
    public String paramName() { return "month"; }

    @Override
    protected YearMonth parse(String raw) {
        try {
            if (raw == null || !raw.matches("\\d{4}-\\d{2}")) throw new DateTimeParseException("", "", 0);
            return YearMonth.parse(raw);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("Mês inválido, use o formato 2027-07");
        }
    }

    protected List<Reservation> startingIn(YearMonth month, Predicate<Reservation> filter) {
        return reservations.select(r -> YearMonth.from(r.getCheckIn()).equals(month) && filter.test(r))
                .stream().sorted(BY_CHECK_IN_THEN_ID).toList();
    }

    /** As reservas que deixaram dinheiro com a pousada. */
    protected List<Reservation> retainingMoney(YearMonth month) {
        return startingIn(month, r -> r.retained().signum() > 0);
    }
}

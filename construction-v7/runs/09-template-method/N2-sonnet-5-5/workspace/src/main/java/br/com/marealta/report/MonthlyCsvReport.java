package br.com.marealta.report;

import br.com.marealta.domain.Reservation;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

/** Base dos relatórios de um mês, sobre as reservas que começam nele. */
abstract class MonthlyCsvReport extends CsvReport<YearMonth> {
    @Override
    public String parameter() { return "month"; }

    @Override
    protected YearMonth parse(String period) {
        if (!period.matches("\\d{4}-\\d{2}")) throw new InvalidPeriodException("Mês inválido: " + period);
        try {
            return YearMonth.parse(period);
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("Mês inválido: " + period);
        }
    }

    /** Reservas que começam no mês, por data de entrada e depois por número. */
    protected static List<Reservation> startingIn(YearMonth month, List<Reservation> all) {
        return all.stream()
                .filter(r -> YearMonth.from(r.checkInDate()).equals(month))
                .sorted(Comparator.comparing(Reservation::checkInDate).thenComparingInt(Reservation::id))
                .toList();
    }

    /** As que deixaram dinheiro com a pousada (base do financeiro e dos planos). */
    protected static List<Reservation> retainingMoney(YearMonth month, List<Reservation> all) {
        return startingIn(month, all).stream().filter(r -> r.retained().signum() > 0).toList();
    }
}

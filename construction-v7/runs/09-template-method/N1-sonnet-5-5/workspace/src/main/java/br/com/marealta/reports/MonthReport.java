package br.com.marealta.reports;

import br.com.marealta.ApiException;
import br.com.marealta.Reservation;
import br.com.marealta.ReservationService;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

/** Relatório de um mês, sobre as reservas que começam nele. */
abstract class MonthReport extends CsvReport<YearMonth> {
    private final ReservationService reservations;

    MonthReport(String name, ReservationService reservations) {
        super(name, "month");
        this.reservations = reservations;
    }

    @Override
    YearMonth parse(String value) {
        if (value == null || !value.matches("\\d{4}-\\d{2}")) {
            throw ApiException.invalid("month deve estar no formato yyyy-MM");
        }
        try {
            return YearMonth.parse(value);
        } catch (java.time.DateTimeException e) {
            throw ApiException.invalid("month deve estar no formato yyyy-MM");
        }
    }

    @Override
    String label(YearMonth month) {
        return month.toString();
    }

    /** Reservas que começam no mês, por data de entrada e depois por número. */
    List<Reservation> startingIn(YearMonth month) {
        return reservations.all().stream()
                .filter(r -> YearMonth.from(r.checkIn()).equals(month))
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparingInt(Reservation::id))
                .toList();
    }

    /** Reservas que deixaram dinheiro com a pousada (base do financeiro e dos planos). */
    List<Reservation> retainingMoneyIn(YearMonth month) {
        return startingIn(month).stream().filter(r -> r.retained().signum() > 0).toList();
    }
}

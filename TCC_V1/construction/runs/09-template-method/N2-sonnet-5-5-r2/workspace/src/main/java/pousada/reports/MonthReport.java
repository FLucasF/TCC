package pousada.reports;

import pousada.domain.Reservation;
import pousada.repo.ReservationRepository;

import java.time.YearMonth;
import java.util.List;

/** Relatório de um mês, sobre as reservas que começam nele. */
abstract class MonthReport extends Report<YearMonth> {
    private final ReservationRepository reservations;

    MonthReport(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    @Override
    public String periodParam() {
        return "month";
    }

    @Override
    protected YearMonth parsePeriod(String raw) {
        return YearMonth.parse(raw);
    }

    protected List<Reservation> startingIn(YearMonth month) {
        return reservations.findCheckInBetween(month.atDay(1), month.atEndOfMonth());
    }

    /** As reservas que deixaram dinheiro com a pousada. */
    protected List<Reservation> withRetainedMoney(YearMonth month) {
        return startingIn(month).stream().filter(r -> r.retained().signum() > 0).toList();
    }
}

package pousada.report;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

import pousada.db.ReservationRepository;
import pousada.domain.Reservation;
import pousada.error.ApiException;

/** Relatório de um mês, pedido como ?month=2027-07. */
abstract class MonthlyReport implements Report {
    protected final ReservationRepository reservations;

    protected MonthlyReport(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    @Override
    public final String parameter() {
        return "month";
    }

    @Override
    public final ReportContent generate(String period) {
        YearMonth month;
        try {
            month = YearMonth.parse(period);
        } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.invalidRequest("Mês fora do formato AAAA-MM");
        }
        return generate(month);
    }

    protected abstract ReportContent generate(YearMonth month);

    /** As reservas do mês que deixaram dinheiro com a pousada: a base do financeiro e dos planos. */
    protected final List<Reservation> withMoneyKept(YearMonth month) {
        return reservations.startingIn(month).stream().filter(r -> r.kept().signum() > 0).toList();
    }
}

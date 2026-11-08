package br.com.marealta.report;

import br.com.marealta.domain.Reservation;
import br.com.marealta.store.Store;
import java.time.YearMonth;
import java.util.List;

/** Base dos relatórios de um mês, sobre as reservas que começam nele. */
abstract class MonthlyReport implements Report {
    protected final Store store;

    MonthlyReport(Store store) {
        this.store = store;
    }

    @Override
    public String parameter() {
        return "month";
    }

    @Override
    public final ReportData generate(String period) {
        YearMonth month = Periods.month(period);
        List<Reservation> starting = store.transaction(tx -> tx.startingBetween(month.atDay(1), month.atEndOfMonth()));
        return build(month.toString(), starting);
    }

    protected abstract ReportData build(String period, List<Reservation> startingInMonth);

    /** Reservas que deixaram dinheiro com a pousada (pago menos devolvido > 0). */
    protected static List<Reservation> retainingMoney(List<Reservation> reservations) {
        return reservations.stream().filter(r -> r.retainedAmount().signum() > 0).toList();
    }
}

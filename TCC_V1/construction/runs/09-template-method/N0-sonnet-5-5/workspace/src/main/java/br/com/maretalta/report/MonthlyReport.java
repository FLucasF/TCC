package br.com.maretalta.report;

import br.com.maretalta.domain.Reservation;
import br.com.maretalta.service.ApiException;
import br.com.maretalta.store.Store;

import java.time.YearMonth;
import java.util.List;

/** Base dos relatórios de um mês (?month=2027-07). */
abstract class MonthlyReport implements ReportType {

    @Override public String periodParam() { return "month"; }

    @Override
    public final Report build(Store.Tx tx, String rawPeriod) {
        if (rawPeriod == null || !rawPeriod.matches("\\d{4}-\\d{2}")) {
            throw ApiException.invalid("Informe o mês no formato 2027-07");
        }
        YearMonth month;
        try {
            month = YearMonth.parse(rawPeriod);
        } catch (RuntimeException e) {
            throw ApiException.invalid("Mês inválido: " + rawPeriod);
        }
        return build(tx, month);
    }

    protected abstract Report build(Store.Tx tx, YearMonth month);

    /** Reservas que começam no mês e deixaram dinheiro com a pousada (pago menos devolvido). */
    protected static List<Reservation> retainedMoney(Store.Tx tx, YearMonth month) {
        return tx.startingIn(month).stream().filter(r -> r.retained().signum() > 0).toList();
    }

    /** Reservas pagas que começam no mês. */
    protected static List<Reservation> paid(Store.Tx tx, YearMonth month) {
        return tx.startingIn(month).stream().filter(r -> r.status().isPaid()).toList();
    }
}

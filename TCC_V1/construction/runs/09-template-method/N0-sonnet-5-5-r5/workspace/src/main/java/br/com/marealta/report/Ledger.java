package br.com.marealta.report;

import br.com.marealta.domain.Reservation;
import br.com.marealta.store.Tx;
import java.time.YearMonth;
import java.util.List;

/** Seleções de reservas compartilhadas pelos relatórios mensais. */
final class Ledger {
    private Ledger() {}

    static List<Reservation> startingIn(Tx tx, YearMonth month) {
        return tx.startingBetween(month.atDay(1), month.plusMonths(1).atDay(1));
    }

    /** Reservas do mês que foram pagas (e não canceladas). */
    static List<Reservation> paidStartingIn(Tx tx, YearMonth month) {
        return startingIn(tx, month).stream().filter(r -> r.status().isPaid()).toList();
    }

    /** Reservas do mês que deixaram dinheiro com a pousada (pago menos devolvido). */
    static List<Reservation> keptStartingIn(Tx tx, YearMonth month) {
        return startingIn(tx, month).stream().filter(r -> r.kept().signum() > 0).toList();
    }
}

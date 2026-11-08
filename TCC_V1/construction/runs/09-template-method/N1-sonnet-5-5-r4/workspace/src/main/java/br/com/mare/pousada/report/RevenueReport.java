package br.com.mare.pousada.report;

import br.com.mare.pousada.domain.Money;
import br.com.mare.pousada.domain.Reservation;
import br.com.mare.pousada.infra.ReservationRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;

public class RevenueReport extends MonthlyReport {
    private final ReservationRepository reservations;

    public RevenueReport(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    @Override
    public String name() {
        return "revenue";
    }

    /** As reservas do mês que deixaram dinheiro com a pousada; também usadas pelo relatório de planos. */
    static List<Reservation> retainingMoney(ReservationRepository reservations, Connection connection,
                                            YearMonth month) throws SQLException {
        return reservations.findStartingIn(connection, month).stream()
                .filter(r -> r.retainedAmount().signum() > 0)
                .toList();
    }

    @Override
    protected Table build(Connection connection, YearMonth month) throws SQLException {
        List<Reservation> found = retainingMoney(reservations, connection, month);
        BigDecimal sum = found.stream().map(Reservation::retainedAmount).reduce(Money.ZERO, BigDecimal::add);
        List<List<String>> rows = found.stream()
                .map(r -> List.of(String.valueOf(r.getId()), String.valueOf(r.getRoomNumber()),
                        r.getCheckIn().toString(), r.getCheckOut().toString(), r.getStatus().name(),
                        Money.plain(r.retainedAmount())))
                .toList();
        return new Table(List.of("reservation", "room", "check_in", "check_out", "status", "amount"), rows,
                List.of("total", Money.plain(sum)));
    }
}

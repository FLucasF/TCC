package br.com.mare.pousada.report;

import br.com.mare.pousada.domain.Money;
import br.com.mare.pousada.domain.RatePlan;
import br.com.mare.pousada.domain.Reservation;
import br.com.mare.pousada.infra.ReservationRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

public class RatePlanReport extends MonthlyReport {
    private final ReservationRepository reservations;

    public RatePlanReport(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    @Override
    public String name() {
        return "rate-plans";
    }

    @Override
    protected Table build(Connection connection, YearMonth month) throws SQLException {
        List<Reservation> found = RevenueReport.retainingMoney(reservations, connection, month);
        List<List<String>> rows = Arrays.stream(RatePlan.values())
                .map(plan -> {
                    List<Reservation> ofPlan = found.stream().filter(r -> r.getRatePlan() == plan).toList();
                    return List.of(plan.name(), String.valueOf(ofPlan.size()), Money.plain(sumOf(ofPlan)));
                })
                .toList();
        return new Table(List.of("rate_plan", "reservations", "amount"), rows,
                List.of("total", String.valueOf(found.size()), Money.plain(sumOf(found))));
    }

    private static BigDecimal sumOf(List<Reservation> list) {
        return list.stream().map(Reservation::retainedAmount).reduce(Money.ZERO, BigDecimal::add);
    }
}

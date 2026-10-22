package br.com.mealta.report;

import br.com.mealta.domain.Money;
import br.com.mealta.domain.RatePlan;
import br.com.mealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

/** Quantas reservas e quanto dinheiro cada plano de tarifa trouxe no mês. */
public class RatePlanReport implements Report {
    private final YearMonth month;
    private final List<Reservation> kept;

    public RatePlanReport(YearMonth month, List<Reservation> startingInMonth) {
        this.month = month;
        this.kept = RevenueReport.keptMoney(startingInMonth);
    }

    @Override
    public String name() {
        return "rate-plans";
    }

    @Override
    public String period() {
        return month.toString();
    }

    @Override
    public List<String> columns() {
        return List.of("rate_plan", "reservations", "amount");
    }

    @Override
    public List<List<String>> rows() {
        return Arrays.stream(RatePlan.values())
                .map(plan -> {
                    List<Reservation> ofPlan = kept.stream().filter(r -> r.ratePlan() == plan).toList();
                    return List.of(plan.name(), String.valueOf(ofPlan.size()), Money.plain(sum(ofPlan)));
                })
                .toList();
    }

    @Override
    public List<String> total() {
        return List.of("total", String.valueOf(kept.size()), Money.plain(sum(kept)));
    }

    private static BigDecimal sum(List<Reservation> reservations) {
        return reservations.stream().map(Reservation::keptAmount).reduce(Money.ZERO, BigDecimal::add);
    }
}

package br.com.marealta.reservas.reports;

import br.com.marealta.reservas.domain.Money;
import br.com.marealta.reservas.domain.RatePlan;
import br.com.marealta.reservas.domain.Reservation;
import br.com.marealta.reservas.domain.ReservationService;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class RatePlanReport implements Report {
    private final ReservationService service;

    public RatePlanReport(ReservationService service) {
        this.service = service;
    }

    @Override
    public String name() {
        return "rate-plans";
    }

    @Override
    public String periodParam() {
        return "month";
    }

    @Override
    public ReportTable build(String period) {
        YearMonth month = Periods.month(period);
        List<Reservation> kept = ReservationQueries.keptMoneyStartingIn(service, month);
        List<List<String>> rows = new ArrayList<>();
        int totalCount = 0;
        BigDecimal totalAmount = Money.ZERO;
        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> ofPlan = kept.stream().filter(r -> r.ratePlan() == plan).toList();
            BigDecimal amount = ofPlan.stream().map(Reservation::amountKept).reduce(Money.ZERO, BigDecimal::add);
            rows.add(List.of(plan.name(), String.valueOf(ofPlan.size()), Money.plain(amount)));
            totalCount += ofPlan.size();
            totalAmount = totalAmount.add(amount);
        }
        return new ReportTable(name(), month.toString(), List.of("rate_plan", "reservations", "amount"), rows,
                List.of("total", String.valueOf(totalCount), Money.plain(totalAmount)));
    }
}

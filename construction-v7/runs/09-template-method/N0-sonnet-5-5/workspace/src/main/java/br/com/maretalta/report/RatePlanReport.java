package br.com.maretalta.report;

import br.com.maretalta.ReservationService;
import br.com.maretalta.domain.Money;
import br.com.maretalta.domain.RatePlan;
import br.com.maretalta.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class RatePlanReport implements Report {
    private final ReservationService service;

    public RatePlanReport(ReservationService service) {
        this.service = service;
    }

    @Override public String name() { return "rate-plans"; }

    @Override public String paramName() { return "month"; }

    @Override
    public String generate(String rawParam) {
        YearMonth month = Csv.parseMonth(rawParam);
        return service.read(all -> {
            List<Reservation> rs = Csv.revenueReservations(all, month);
            List<String> rows = new ArrayList<>();
            int totalCount = 0;
            BigDecimal totalAmount = Money.ZERO;
            for (RatePlan plan : RatePlan.values()) {
                List<Reservation> ofPlan = rs.stream().filter(r -> r.ratePlan == plan).toList();
                BigDecimal amount = ofPlan.stream().map(Reservation::netAmount).reduce(Money.ZERO, BigDecimal::add);
                rows.add(plan.name() + "," + ofPlan.size() + "," + Money.plain(amount));
                totalCount += ofPlan.size();
                totalAmount = totalAmount.add(amount);
            }
            return Csv.build(name(), month.toString(), "rate_plan,reservations,amount", rows,
                    "total," + totalCount + "," + Money.plain(totalAmount));
        });
    }
}

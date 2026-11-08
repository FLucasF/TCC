package com.pousadamarealta.report;

import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.util.MoneyUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RatePlansReport extends BaseReport {
    private final ReservationRepository reservationRepository;

    public RatePlansReport() {
        this.reservationRepository = new ReservationRepository();
    }

    public String generate(int year, int month) {
        addTitleLine("rate-plans", String.format("%04d-%02d", year, month));
        addHeaderLine("rate_plan", "reservations", "amount");

        List<Reservation> reservations = reservationRepository.findByMonth(year, month);

        Map<RatePlan, Integer> counts = new HashMap<>();
        Map<RatePlan, BigDecimal> amounts = new HashMap<>();

        for (RatePlan plan : RatePlan.values()) {
            counts.put(plan, 0);
            amounts.put(plan, BigDecimal.ZERO);
        }

        for (Reservation r : reservations) {
            BigDecimal netAmount = r.getAmountPaid();
            if (r.getRefund() != null) {
                netAmount = netAmount.subtract(r.getRefund());
            }

            if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                RatePlan plan = r.getRatePlan();
                counts.put(plan, counts.get(plan) + 1);
                amounts.put(plan, amounts.get(plan).add(netAmount));
            }
        }

        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (RatePlan plan : RatePlan.values()) {
            int count = counts.get(plan);
            BigDecimal amount = amounts.get(plan);

            addDataLine(
                plan.name(),
                String.valueOf(count),
                MoneyUtils.formatCSV(amount)
            );

            totalCount += count;
            totalAmount = totalAmount.add(amount);
        }

        addDataLine("total", String.valueOf(totalCount), MoneyUtils.formatCSV(totalAmount));

        return super.generate();
    }
}

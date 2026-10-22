package com.pousada.service.report;

import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.repo.ReservationRepository;
import com.pousada.util.Money;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RatePlansReport extends Report {
    private final ReservationRepository reservationRepo;
    private final int year;
    private final int month;

    public RatePlansReport(ReservationRepository reservationRepo, YearMonth yearMonth) throws Exception {
        super("rate-plans", yearMonth.toString());
        this.reservationRepo = reservationRepo;
        this.year = yearMonth.getYear();
        this.month = yearMonth.getMonthValue();
    }

    @Override
    protected String getColumnHeader() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected List<String> getDataRows() {
        List<String> rows = new ArrayList<>();
        try {
            Map<RatePlan, Integer> countByPlan = new HashMap<>();
            Map<RatePlan, BigDecimal> amountByPlan = new HashMap<>();

            for (RatePlan plan : RatePlan.values()) {
                countByPlan.put(plan, 0);
                amountByPlan.put(plan, BigDecimal.ZERO);
            }

            List<Reservation> reservations = reservationRepo.findByMonthAndPaid(year, month);
            for (Reservation res : reservations) {
                RatePlan plan = res.getRatePlan();
                countByPlan.put(plan, countByPlan.get(plan) + 1);
                BigDecimal net = Money.subtract(res.getAmountPaid(), res.getRefund() != null ? res.getRefund() : BigDecimal.ZERO);
                amountByPlan.put(plan, Money.add(amountByPlan.get(plan), net));
            }

            for (RatePlan plan : RatePlan.values()) {
                rows.add(formatRow(plan, countByPlan.get(plan), amountByPlan.get(plan)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    @Override
    protected String getTotalLine() {
        try {
            List<Reservation> reservations = reservationRepo.findByMonthAndPaid(year, month);
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (Reservation res : reservations) {
                BigDecimal net = Money.subtract(res.getAmountPaid(), res.getRefund() != null ? res.getRefund() : BigDecimal.ZERO);
                totalAmount = Money.add(totalAmount, net);
            }
            return "total," + reservations.size() + "," + formatMoney(totalAmount);
        } catch (Exception e) {
            return "total,0,0.00";
        }
    }

    private String formatRow(RatePlan plan, int count, BigDecimal amount) {
        return plan.getDisplayName() + "," + count + "," + formatMoney(amount);
    }

    private String formatMoney(BigDecimal value) {
        return String.format("%.2f", value);
    }
}

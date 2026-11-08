package com.pousadamareaalta.service.report;

import com.pousadamareaalta.database.*;
import com.pousadamareaalta.domain.*;
import com.pousadamareaalta.util.FormatterUtil;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.*;

public class RatePlansReport implements ReportGenerator {
    private final YearMonth month;

    public RatePlansReport(YearMonth month) {
        this.month = month;
    }

    @Override
    public String generate() throws SQLException {
        List<Reservation> reservations = ReservationRepository.findByMonthStarting(month.getYear(), month.getMonthValue());

        Map<String, Long> countByPlan = new HashMap<>();
        Map<String, Double> amountByPlan = new HashMap<>();

        countByPlan.put("STANDARD", 0L);
        countByPlan.put("NON_REFUNDABLE", 0L);
        amountByPlan.put("STANDARD", 0.0);
        amountByPlan.put("NON_REFUNDABLE", 0.0);

        for (Reservation res : reservations) {
            String plan = res.ratePlan();
            countByPlan.put(plan, countByPlan.getOrDefault(plan, 0L) + 1);

            double amount = res.amountPaid();
            if (res.refund() != null) {
                amount -= res.refund();
            }
            amountByPlan.put(plan, amountByPlan.getOrDefault(plan, 0.0) + amount);
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(month.getYear()).append("-");
        if (month.getMonthValue() < 10) csv.append("0");
        csv.append(month.getMonthValue()).append("\n");

        csv.append("rate_plan,reservations,amount\n");

        long totalReservations = 0;
        double totalAmount = 0.0;

        for (String plan : Arrays.asList("STANDARD", "NON_REFUNDABLE")) {
            long count = countByPlan.get(plan);
            double amount = amountByPlan.get(plan);

            csv.append(plan).append(",");
            csv.append(count).append(",");
            csv.append(FormatterUtil.formatCurrencyCSV(amount)).append("\n");

            totalReservations += count;
            totalAmount += amount;
        }

        csv.append("total,").append(totalReservations).append(",").append(FormatterUtil.formatCurrencyCSV(totalAmount));

        return csv.toString();
    }

    @Override
    public String getReportName() {
        return "rate-plans";
    }
}

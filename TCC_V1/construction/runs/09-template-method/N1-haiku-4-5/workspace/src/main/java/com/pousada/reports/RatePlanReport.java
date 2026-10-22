package com.pousada.reports;

import com.pousada.db.ReservationRepository;
import com.pousada.models.RatePlan;
import com.pousada.models.Reservation;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RatePlanReport extends Report {
    private final ReservationRepository reservationRepo;
    private final int month;
    private final int year;

    public RatePlanReport(ReservationRepository reservationRepo, int month, int year) {
        this.reservationRepo = reservationRepo;
        this.month = month;
        this.year = year;
    }

    @Override
    public String generate() {
        try {
            StringBuilder sb = new StringBuilder();
            String period = String.format("%04d-%02d", year, month);
            String title = generateTitle("rate-plans", period);
            sb.append(title).append("\n");

            sb.append(generateCsvRow("rate_plan", "reservations", "amount")).append("\n");

            Map<RatePlan, Integer> reservationCounts = new HashMap<>();
            Map<RatePlan, BigDecimal> amounts = new HashMap<>();

            reservationCounts.put(RatePlan.STANDARD, 0);
            reservationCounts.put(RatePlan.NON_REFUNDABLE, 0);
            amounts.put(RatePlan.STANDARD, BigDecimal.ZERO);
            amounts.put(RatePlan.NON_REFUNDABLE, BigDecimal.ZERO);

            List<Reservation> reservations = reservationRepo.findByMonthStarting(month, year);

            for (Reservation res : reservations) {
                RatePlan plan = res.getRatePlan();
                reservationCounts.put(plan, reservationCounts.get(plan) + 1);

                BigDecimal netAmount = res.getAmountPaid();
                if (res.getRefund() != null) {
                    netAmount = netAmount.subtract(res.getRefund());
                }
                amounts.put(plan, amounts.get(plan).add(netAmount));
            }

            BigDecimal totalAmount = BigDecimal.ZERO;
            int totalReservations = 0;

            for (RatePlan plan : RatePlan.values()) {
                int count = reservationCounts.get(plan);
                BigDecimal amount = amounts.get(plan);
                totalReservations += count;
                totalAmount = totalAmount.add(amount);

                sb.append(generateCsvRow(
                        plan.getLabel(),
                        String.valueOf(count),
                        String.format("%.2f", amount)
                )).append("\n");
            }

            sb.append(generateCsvRow(
                    "total",
                    String.valueOf(totalReservations),
                    String.format("%.2f", totalAmount)
            )).append("\n");

            return sb.toString();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

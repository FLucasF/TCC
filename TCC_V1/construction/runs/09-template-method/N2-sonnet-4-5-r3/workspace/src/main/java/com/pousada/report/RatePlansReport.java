package com.pousada.report;

import com.pousada.domain.Reservation;
import com.pousada.domain.rateplan.RatePlan;
import com.pousada.domain.rateplan.RatePlanFactory;
import com.pousada.repository.ReservationRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RatePlansReport extends Report {
    private final int year;
    private final int month;
    private final ReservationRepository reservationRepository;
    private Map<String, PlanStats> planStats;
    private int totalReservations;
    private BigDecimal totalAmount;

    public RatePlansReport(int year, int month, ReservationRepository reservationRepository) {
        this.year = year;
        this.month = month;
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String getReportName() {
        return "rate-plans";
    }

    @Override
    protected String getPeriod() {
        return String.format("%04d-%02d", year, month);
    }

    @Override
    protected String buildHeaderLine() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected List<String> buildDataLines() {
        planStats = new LinkedHashMap<>();

        for (Map.Entry<String, RatePlan> entry : RatePlanFactory.getAll().entrySet()) {
            planStats.put(entry.getKey(), new PlanStats());
        }

        List<Reservation> reservations = reservationRepository.findReservationsStartingInMonth(year, month);

        for (Reservation reservation : reservations) {
            BigDecimal revenue = calculateRevenue(reservation);
            PlanStats stats = planStats.get(reservation.getRatePlan().getName());
            stats.count++;
            stats.amount = stats.amount.add(revenue);
        }

        totalReservations = 0;
        totalAmount = BigDecimal.ZERO;

        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, PlanStats> entry : planStats.entrySet()) {
            PlanStats stats = entry.getValue();
            totalReservations += stats.count;
            totalAmount = totalAmount.add(stats.amount);

            lines.add(String.join(",",
                entry.getKey(),
                String.valueOf(stats.count),
                formatAmount(stats.amount)
            ));
        }

        return lines;
    }

    @Override
    protected String buildTotalLine() {
        return String.format("total,%d,%s", totalReservations, formatAmount(totalAmount));
    }

    private BigDecimal calculateRevenue(Reservation reservation) {
        BigDecimal paid = reservation.getAmountPaid();
        BigDecimal refunded = reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refunded);
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }

    private static class PlanStats {
        int count = 0;
        BigDecimal amount = BigDecimal.ZERO;
    }
}

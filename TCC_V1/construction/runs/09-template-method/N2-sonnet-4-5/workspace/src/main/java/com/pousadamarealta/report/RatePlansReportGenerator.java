package com.pousadamarealta.report;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RatePlansReportGenerator extends ReportGenerator {
    private final YearMonth month;
    private final ReservationRepository reservationRepository;

    public RatePlansReportGenerator(YearMonth month, ReservationRepository reservationRepository) {
        this.month = month;
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String getReportName() {
        return "rate-plans";
    }

    @Override
    protected String getPeriod() {
        return formatMonth(month);
    }

    @Override
    protected String buildHeaderLine() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected String buildDataLines() {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);

        Map<RatePlan, List<Reservation>> byPlan = reservations.stream()
            .filter(r -> r.getNetRevenue().compareTo(BigDecimal.ZERO) > 0)
            .collect(Collectors.groupingBy(Reservation::getRatePlan));

        StringBuilder lines = new StringBuilder();

        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> planReservations = byPlan.getOrDefault(plan, List.of());
            long count = planReservations.size();
            BigDecimal amount = planReservations.stream()
                .map(Reservation::getNetRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            lines.append(plan.name()).append(",");
            lines.append(count).append(",");
            lines.append(formatDecimal(amount)).append("\n");
        }

        return lines.toString();
    }

    @Override
    protected String buildTotalLine() {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);

        long totalCount = reservations.stream()
            .filter(r -> r.getNetRevenue().compareTo(BigDecimal.ZERO) > 0)
            .count();

        BigDecimal totalAmount = reservations.stream()
            .map(Reservation::getNetRevenue)
            .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return "total," + totalCount + "," + formatDecimal(totalAmount);
    }
}

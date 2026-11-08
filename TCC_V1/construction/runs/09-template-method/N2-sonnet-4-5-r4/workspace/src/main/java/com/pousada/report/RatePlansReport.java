package com.pousada.report;

import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RatePlansReport extends ReportGenerator {
    private final ReservationRepository reservationRepository;

    public RatePlansReport(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String getReportName() {
        return "rate-plans";
    }

    @Override
    protected String generateHeader() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected List<String> generateDataLines(String period) {
        YearMonth month = YearMonth.parse(period);
        List<Reservation> reservations = reservationRepository.findRevenueReservationsForMonth(month);

        Map<RatePlan, List<Reservation>> byPlan = reservations.stream()
            .collect(Collectors.groupingBy(Reservation::getRatePlan));

        List<String> lines = new ArrayList<>();

        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> planReservations = byPlan.getOrDefault(plan, List.of());
            int count = planReservations.size();
            BigDecimal amount = planReservations.stream()
                .map(r -> r.getAmountPaid().subtract(
                    r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO
                ))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            lines.add(String.format("%s,%d,%s", plan, count, formatMoney(amount)));
        }

        return lines;
    }

    @Override
    protected String generateTotal(String period) {
        YearMonth month = YearMonth.parse(period);
        List<Reservation> reservations = reservationRepository.findRevenueReservationsForMonth(month);

        int totalCount = reservations.size();
        BigDecimal totalAmount = reservations.stream()
            .map(r -> r.getAmountPaid().subtract(
                r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO
            ))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return String.format("total,%d,%s", totalCount, formatMoney(totalAmount));
    }
}

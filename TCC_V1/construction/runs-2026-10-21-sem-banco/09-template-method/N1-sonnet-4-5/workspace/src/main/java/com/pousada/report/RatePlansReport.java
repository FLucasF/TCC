package com.pousada.report;

import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RatePlansReport extends Report {
    private final YearMonth month;
    private final ReservationRepository reservationRepository;

    public RatePlansReport(YearMonth month, ReservationRepository reservationRepository) {
        this.month = month;
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String getReportName() {
        return "rate-plans";
    }

    @Override
    protected String getPeriod() {
        return month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    @Override
    protected String buildHeaderLine() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected List<String> buildDataLines() {
        List<String> lines = new ArrayList<>();
        List<Reservation> monthReservations = reservationRepository.findByCheckInMonth(month);

        Map<RatePlan, List<Reservation>> byPlan = monthReservations.stream()
                .filter(r -> r.getNetAmount().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.groupingBy(Reservation::getRatePlan));

        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> planReservations = byPlan.getOrDefault(plan, List.of());
            int count = planReservations.size();
            BigDecimal amount = planReservations.stream()
                    .map(Reservation::getNetAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            lines.add(String.format("%s,%d,%s",
                    plan.name(),
                    count,
                    formatAmount(amount)
            ));
        }

        return lines;
    }

    @Override
    protected String buildTotalLine() {
        List<Reservation> monthReservations = reservationRepository.findByCheckInMonth(month);

        int totalCount = (int) monthReservations.stream()
                .filter(r -> r.getNetAmount().compareTo(BigDecimal.ZERO) > 0)
                .count();

        BigDecimal totalAmount = monthReservations.stream()
                .map(Reservation::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return String.format("total,%d,%s", totalCount, formatAmount(totalAmount));
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}

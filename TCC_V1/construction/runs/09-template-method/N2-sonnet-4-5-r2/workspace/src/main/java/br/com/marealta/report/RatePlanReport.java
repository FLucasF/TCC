package br.com.marealta.report;

import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import br.com.marealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RatePlanReport extends CsvReport {
    private final YearMonth month;
    private final ReservationRepository reservationRepository;
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

    public RatePlanReport(YearMonth month, ReservationRepository reservationRepository) {
        this.month = month;
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String getReportName() {
        return "rate-plans";
    }

    @Override
    protected String getPeriod() {
        return month.format(monthFormatter);
    }

    @Override
    protected String buildHeaderLine() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected List<String> buildDataLines() {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);

        Map<RatePlan, List<Reservation>> byPlan = reservations.stream()
                .filter(r -> r.getNetAmount().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.groupingBy(Reservation::getRatePlan));

        List<String> lines = new ArrayList<>();

        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> planReservations = byPlan.getOrDefault(plan, List.of());
            int count = planReservations.size();
            BigDecimal amount = planReservations.stream()
                    .map(Reservation::getNetAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            lines.add(String.join(",",
                    plan.name(),
                    String.valueOf(count),
                    formatAmount(amount)
            ));
        }

        return lines;
    }

    @Override
    protected String buildTotalLine() {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);

        long totalCount = reservations.stream()
                .filter(r -> r.getNetAmount().compareTo(BigDecimal.ZERO) > 0)
                .count();

        BigDecimal totalAmount = reservations.stream()
                .map(Reservation::getNetAmount)
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return "total," + totalCount + "," + formatAmount(totalAmount);
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}

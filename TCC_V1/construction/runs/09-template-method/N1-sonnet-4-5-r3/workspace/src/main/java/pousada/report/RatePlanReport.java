package pousada.report;

import pousada.domain.RatePlan;
import pousada.domain.Reservation;
import pousada.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RatePlanReport implements Report {
    private final YearMonth month;
    private final Map<RatePlan, List<Reservation>> reservationsByPlan;

    public RatePlanReport(YearMonth month, ReservationRepository reservationRepository) {
        this.month = month;
        List<Reservation> allReservations = reservationRepository.findRevenueReservationsForMonth(month);
        this.reservationsByPlan = allReservations.stream()
            .collect(Collectors.groupingBy(Reservation::getRatePlan));
    }

    @Override
    public String getName() {
        return "rate-plans";
    }

    @Override
    public String getPeriod() {
        return month.toString();
    }

    @Override
    public List<String> getColumns() {
        return List.of("rate_plan", "reservations", "amount");
    }

    @Override
    public List<Row> getDataRows() {
        List<Row> rows = new ArrayList<>();

        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> reservations = reservationsByPlan.getOrDefault(plan, List.of());
            int count = reservations.size();
            BigDecimal amount = reservations.stream()
                .map(this::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            rows.add(new Row(List.of(
                plan.name(),
                String.valueOf(count),
                formatAmount(amount)
            )));
        }

        return rows;
    }

    @Override
    public Row getTotalRow() {
        int totalCount = reservationsByPlan.values().stream()
            .mapToInt(List::size)
            .sum();

        BigDecimal totalAmount = reservationsByPlan.values().stream()
            .flatMap(List::stream)
            .map(this::getNetAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Row(List.of("total", String.valueOf(totalCount), formatAmount(totalAmount)));
    }

    private BigDecimal getNetAmount(Reservation r) {
        BigDecimal refund = r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO;
        return r.getAmountPaid().subtract(refund);
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}

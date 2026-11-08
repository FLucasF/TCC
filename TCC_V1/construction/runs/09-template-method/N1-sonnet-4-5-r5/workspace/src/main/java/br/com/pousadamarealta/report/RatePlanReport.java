package br.com.pousadamarealta.report;

import br.com.pousadamarealta.domain.reservation.RatePlan;
import br.com.pousadamarealta.domain.reservation.Reservation;
import br.com.pousadamarealta.domain.reservation.ReservationRepository;
import br.com.pousadamarealta.util.MoneyUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class RatePlanReport extends ReportGenerator {
    private final ReservationRepository reservationRepository;
    private final int year;
    private final int month;
    private Map<RatePlan, List<Reservation>> reservationsByPlan;

    public RatePlanReport(ReservationRepository reservationRepository, int year, int month) {
        this.reservationRepository = reservationRepository;
        this.year = year;
        this.month = month;
    }

    @Override
    protected String getReportName() {
        return "rate-plans";
    }

    @Override
    protected List<String> getHeaders() {
        return Arrays.asList("rate_plan", "reservations", "amount");
    }

    @Override
    protected List<String> getDataLines() {
        try {
            List<Reservation> allReservations = reservationRepository.findByCheckInMonth(year, month);

            reservationsByPlan = allReservations.stream()
                    .filter(r -> r.getAmountKept().compareTo(BigDecimal.ZERO) > 0)
                    .collect(Collectors.groupingBy(Reservation::getRatePlan));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Garantir que ambos os planos apareçam, mesmo sem reservas
        List<String> lines = new ArrayList<>();
        for (RatePlan plan : List.of(RatePlan.STANDARD, RatePlan.NON_REFUNDABLE)) {
            List<Reservation> reservations = reservationsByPlan.getOrDefault(plan, List.of());
            int count = reservations.size();
            BigDecimal amount = reservations.stream()
                    .map(Reservation::getAmountKept)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            lines.add(String.format("%s,%d,%s",
                    plan.name(),
                    count,
                    MoneyUtils.formatCsv(amount)));
        }

        return lines;
    }

    @Override
    protected String getTotalLine() {
        int totalCount = reservationsByPlan.values().stream()
                .mapToInt(List::size)
                .sum();

        BigDecimal totalAmount = reservationsByPlan.values().stream()
                .flatMap(List::stream)
                .map(Reservation::getAmountKept)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return String.format("total,%d,%s", totalCount, MoneyUtils.formatCsv(totalAmount));
    }
}

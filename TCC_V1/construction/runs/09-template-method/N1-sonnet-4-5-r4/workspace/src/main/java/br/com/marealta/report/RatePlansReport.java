package br.com.marealta.report;

import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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
    protected String buildHeader() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected List<String> buildDataRows(String period) {
        YearMonth yearMonth = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"));

        List<ReservationStatus> statuses = List.of(
            ReservationStatus.CONFIRMED,
            ReservationStatus.CHECKED_IN,
            ReservationStatus.CHECKED_OUT,
            ReservationStatus.CANCELLED
        );

        List<Reservation> reservations = reservationRepository.findByMonthAndStatuses(
            yearMonth.getYear(), yearMonth.getMonthValue(), statuses
        );

        Map<RatePlan, List<Reservation>> byPlan = reservations.stream()
            .collect(Collectors.groupingBy(Reservation::getRatePlan));

        List<String> rows = new ArrayList<>();

        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> planReservations = byPlan.getOrDefault(plan, List.of());
            BigDecimal totalAmount = planReservations.stream()
                .map(this::calculateNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            rows.add(String.format("%s,%d,%s",
                plan.name(),
                planReservations.size(),
                formatDecimal(totalAmount)
            ));
        }

        return rows;
    }

    @Override
    protected String buildTotal(String period) {
        YearMonth yearMonth = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"));

        List<ReservationStatus> statuses = List.of(
            ReservationStatus.CONFIRMED,
            ReservationStatus.CHECKED_IN,
            ReservationStatus.CHECKED_OUT,
            ReservationStatus.CANCELLED
        );

        List<Reservation> reservations = reservationRepository.findByMonthAndStatuses(
            yearMonth.getYear(), yearMonth.getMonthValue(), statuses
        );

        BigDecimal totalAmount = reservations.stream()
            .map(this::calculateNetAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return String.format("total,%d,%s", reservations.size(), formatDecimal(totalAmount));
    }

    private BigDecimal calculateNetAmount(Reservation reservation) {
        BigDecimal paid = reservation.getAmountPaid();
        BigDecimal refund = reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refund);
    }

    private String formatDecimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}

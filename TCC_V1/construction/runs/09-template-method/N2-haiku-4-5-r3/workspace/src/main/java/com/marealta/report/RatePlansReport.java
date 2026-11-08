package com.marealta.report;

import com.marealta.db.ReservationRepository;
import com.marealta.model.Reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RatePlansReport implements Report {
    private final ReservationRepository reservationRepository;

    public RatePlansReport(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    public String generate(LocalDate parameter) {
        YearMonth month = YearMonth.from(parameter);
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth().plusDays(1);

        List<Reservation> reservations = reservationRepository.findByMonthAndStatuses(
            monthStart, monthEnd,
            List.of("CONFIRMED", "CHECKED_IN", "CHECKED_OUT")
        );

        Map<Reservation.RatePlan, Integer> reservationCounts = new HashMap<>();
        Map<Reservation.RatePlan, BigDecimal> amountsByPlan = new HashMap<>();

        reservationCounts.put(Reservation.RatePlan.STANDARD, 0);
        reservationCounts.put(Reservation.RatePlan.NON_REFUNDABLE, 0);
        amountsByPlan.put(Reservation.RatePlan.STANDARD, BigDecimal.ZERO);
        amountsByPlan.put(Reservation.RatePlan.NON_REFUNDABLE, BigDecimal.ZERO);

        for (Reservation res : reservations) {
            Reservation.RatePlan plan = res.getRatePlan();
            BigDecimal amount = res.getAmountPaid().subtract(res.getRefund() != null ? res.getRefund() : BigDecimal.ZERO);

            reservationCounts.put(plan, reservationCounts.get(plan) + 1);
            amountsByPlan.put(plan, amountsByPlan.get(plan).add(amount));
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(month).append("\n");
        csv.append("rate_plan,reservations,amount\n");

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalReservations = 0;

        for (Reservation.RatePlan plan : Reservation.RatePlan.values()) {
            int count = reservationCounts.get(plan);
            BigDecimal amount = amountsByPlan.get(plan);
            csv.append(plan).append(",")
                .append(count).append(",")
                .append(formatMoney(amount)).append("\n");
            totalReservations += count;
            totalAmount = totalAmount.add(amount);
        }

        csv.append("total,").append(totalReservations).append(",").append(formatMoney(totalAmount));

        return csv.toString();
    }

    private String formatMoney(BigDecimal value) {
        return String.format("%.2f", value);
    }
}

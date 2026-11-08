package com.pousadamarealta.report;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class RatePlanReport extends CsvReport {
    private final YearMonth month;
    private final ReservationRepository reservationRepository;
    private final DecimalFormat numberFormatter;

    public RatePlanReport(YearMonth month, ReservationRepository reservationRepository) {
        this.month = month;
        this.reservationRepository = reservationRepository;

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        symbols.setGroupingSeparator(',');
        this.numberFormatter = new DecimalFormat("0.00", symbols);
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
    protected List<String> getColumnNames() {
        return List.of("rate_plan", "reservations", "amount");
    }

    @Override
    protected List<List<String>> getDataRows() {
        List<Reservation> reservations = reservationRepository.findByMonth(month);

        Map<RatePlan, List<Reservation>> byPlan = reservations.stream()
            .filter(r -> r.getNetAmount().compareTo(BigDecimal.ZERO) > 0)
            .collect(Collectors.groupingBy(Reservation::getRatePlan));

        List<List<String>> rows = new ArrayList<>();

        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> planReservations = byPlan.getOrDefault(plan, List.of());
            long count = planReservations.size();
            BigDecimal amount = planReservations.stream()
                .map(Reservation::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            rows.add(List.of(
                plan.name(),
                String.valueOf(count),
                numberFormatter.format(amount)
            ));
        }

        return rows;
    }

    @Override
    protected List<String> getTotalRow() {
        List<Reservation> reservations = reservationRepository.findByMonth(month);

        long totalCount = reservations.stream()
            .filter(r -> r.getNetAmount().compareTo(BigDecimal.ZERO) > 0)
            .count();

        BigDecimal totalAmount = reservations.stream()
            .map(Reservation::getNetAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return List.of("total", String.valueOf(totalCount), numberFormatter.format(totalAmount));
    }
}

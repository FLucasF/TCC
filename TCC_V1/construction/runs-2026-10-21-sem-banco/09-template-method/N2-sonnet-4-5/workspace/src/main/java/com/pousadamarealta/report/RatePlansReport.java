package com.pousadamarealta.report;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RatePlansReport extends CsvReport {
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private final YearMonth month;
    private final ReservationRepository reservationRepository;
    private Map<RatePlan, ReportData> dataBySummary;

    public RatePlansReport(YearMonth month, ReservationRepository reservationRepository) {
        this.month = month;
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String reportName() {
        return "rate-plans";
    }

    @Override
    protected String period() {
        return month.format(MONTH_FORMATTER);
    }

    @Override
    protected String headerLine() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected List<String> dataLines() {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month).stream()
                .filter(r -> r.getNetRevenue().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        dataBySummary = new LinkedHashMap<>();
        dataBySummary.put(RatePlan.STANDARD, new ReportData());
        dataBySummary.put(RatePlan.NON_REFUNDABLE, new ReportData());

        for (Reservation r : reservations) {
            ReportData data = dataBySummary.get(r.getRatePlan());
            data.count++;
            data.amount = data.amount.add(r.getNetRevenue());
        }

        List<String> lines = new ArrayList<>();
        for (Map.Entry<RatePlan, ReportData> entry : dataBySummary.entrySet()) {
            lines.add(String.format("%s,%d,%s",
                entry.getKey().name(),
                entry.getValue().count,
                formatAmount(entry.getValue().amount)
            ));
        }

        return lines;
    }

    @Override
    protected String totalLine() {
        int totalCount = dataBySummary.values().stream()
                .mapToInt(d -> d.count)
                .sum();
        BigDecimal totalAmount = dataBySummary.values().stream()
                .map(d -> d.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, java.math.RoundingMode.HALF_EVEN);
        return String.format("total,%d,%s", totalCount, formatAmount(totalAmount));
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_EVEN).toPlainString();
    }

    private static class ReportData {
        int count = 0;
        BigDecimal amount = BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_EVEN);
    }
}

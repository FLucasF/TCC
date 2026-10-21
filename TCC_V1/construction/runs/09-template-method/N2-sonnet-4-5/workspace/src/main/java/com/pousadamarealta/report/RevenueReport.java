package com.pousadamarealta.report;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RevenueReport extends CsvReport {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private final YearMonth month;
    private final ReservationRepository reservationRepository;
    private List<Reservation> reservations;

    public RevenueReport(YearMonth month, ReservationRepository reservationRepository) {
        this.month = month;
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String reportName() {
        return "revenue";
    }

    @Override
    protected String period() {
        return month.format(MONTH_FORMATTER);
    }

    @Override
    protected String headerLine() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    protected List<String> dataLines() {
        reservations = reservationRepository.findByCheckInMonth(month).stream()
                .filter(r -> r.getNetRevenue().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        List<String> lines = new ArrayList<>();
        for (Reservation r : reservations) {
            lines.add(String.format("%d,%s,%s,%s,%s,%s",
                r.getId(),
                r.getRoomNumber(),
                r.getCheckIn().format(DATE_FORMATTER),
                r.getCheckOut().format(DATE_FORMATTER),
                r.getStatus().name(),
                formatAmount(r.getNetRevenue())
            ));
        }

        return lines;
    }

    @Override
    protected String totalLine() {
        BigDecimal total = reservations.stream()
                .map(Reservation::getNetRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, java.math.RoundingMode.HALF_EVEN);
        return "total," + formatAmount(total);
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_EVEN).toPlainString();
    }
}

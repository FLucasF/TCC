package com.pousada.report;

import com.pousada.domain.Reservation;
import com.pousada.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RevenueReport extends Report {
    private final YearMonth month;
    private final ReservationRepository reservationRepository;

    public RevenueReport(YearMonth month, ReservationRepository reservationRepository) {
        this.month = month;
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String getReportName() {
        return "revenue";
    }

    @Override
    protected String getPeriod() {
        return month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    @Override
    protected String buildHeaderLine() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    protected List<String> buildDataLines() {
        List<String> lines = new ArrayList<>();
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);

        reservations.stream()
                .filter(r -> r.getNetAmount().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(Reservation::getCheckIn)
                        .thenComparing(Reservation::getId))
                .forEach(r -> {
                    lines.add(String.format("%d,%s,%s,%s,%s,%s",
                            r.getId(),
                            r.getRoomNumber(),
                            r.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE),
                            r.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE),
                            r.getStatus().name(),
                            formatAmount(r.getNetAmount())
                    ));
                });

        return lines;
    }

    @Override
    protected String buildTotalLine() {
        BigDecimal total = reservationRepository.findByCheckInMonth(month).stream()
                .map(Reservation::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return "total," + formatAmount(total);
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}

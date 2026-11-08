package br.com.pousadamarealta.report;

import br.com.pousadamarealta.domain.reservation.Reservation;
import br.com.pousadamarealta.domain.reservation.ReservationRepository;
import br.com.pousadamarealta.util.DateUtils;
import br.com.pousadamarealta.util.MoneyUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RevenueReport extends ReportGenerator {
    private final ReservationRepository reservationRepository;
    private final int year;
    private final int month;
    private List<Reservation> reservations;

    public RevenueReport(ReservationRepository reservationRepository, int year, int month) {
        this.reservationRepository = reservationRepository;
        this.year = year;
        this.month = month;
    }

    @Override
    protected String getReportName() {
        return "revenue";
    }

    @Override
    protected List<String> getHeaders() {
        return Arrays.asList("reservation", "room", "check_in", "check_out", "status", "amount");
    }

    @Override
    protected List<String> getDataLines() {
        try {
            reservations = reservationRepository.findByCheckInMonth(year, month);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return reservations.stream()
                .filter(r -> r.getAmountKept().compareTo(BigDecimal.ZERO) > 0)
                .map(r -> String.format("%d,%s,%s,%s,%s,%s",
                        r.getId(),
                        r.getRoomNumber(),
                        DateUtils.formatIso(r.getCheckIn()),
                        DateUtils.formatIso(r.getCheckOut()),
                        r.getStatus().name(),
                        MoneyUtils.formatCsv(r.getAmountKept())))
                .collect(Collectors.toList());
    }

    @Override
    protected String getTotalLine() {
        BigDecimal total = reservations.stream()
                .map(Reservation::getAmountKept)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return "total," + MoneyUtils.formatCsv(total);
    }
}

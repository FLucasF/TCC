package com.pousadamarealta.report;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class RevenueReportGenerator extends ReportGenerator {
    private final YearMonth month;
    private final ReservationRepository reservationRepository;

    public RevenueReportGenerator(YearMonth month, ReservationRepository reservationRepository) {
        this.month = month;
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String getReportName() {
        return "revenue";
    }

    @Override
    protected String getPeriod() {
        return formatMonth(month);
    }

    @Override
    protected String buildHeaderLine() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    protected String buildDataLines() {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);
        StringBuilder lines = new StringBuilder();
        for (Reservation r : reservations) {
            BigDecimal netRevenue = r.getNetRevenue();
            if (netRevenue.compareTo(BigDecimal.ZERO) > 0) {
                lines.append(r.getId()).append(",");
                lines.append(r.getRoomNumber()).append(",");
                lines.append(formatDate(r.getCheckIn())).append(",");
                lines.append(formatDate(r.getCheckOut())).append(",");
                lines.append(r.getStatus()).append(",");
                lines.append(formatDecimal(netRevenue)).append("\n");
            }
        }
        return lines.toString();
    }

    @Override
    protected String buildTotalLine() {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);
        BigDecimal total = reservations.stream()
            .map(Reservation::getNetRevenue)
            .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return "total," + formatDecimal(total);
    }
}

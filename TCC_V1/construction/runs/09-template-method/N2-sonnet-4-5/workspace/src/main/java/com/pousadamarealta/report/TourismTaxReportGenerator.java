package com.pousadamarealta.report;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class TourismTaxReportGenerator extends ReportGenerator {
    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final YearMonth month;
    private final ReservationRepository reservationRepository;

    public TourismTaxReportGenerator(YearMonth month, ReservationRepository reservationRepository) {
        this.month = month;
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String getReportName() {
        return "tourism-tax";
    }

    @Override
    protected String getPeriod() {
        return formatMonth(month);
    }

    @Override
    protected String buildHeaderLine() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected String buildDataLines() {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);
        StringBuilder lines = new StringBuilder();
        for (Reservation r : reservations) {
            if (r.isPaid()) {
                BigDecimal tax = calculateTax(r);
                lines.append(r.getId()).append(",");
                lines.append(r.getRoomNumber()).append(",");
                lines.append(formatDate(r.getCheckIn())).append(",");
                lines.append(formatDate(r.getCheckOut())).append(",");
                lines.append(r.getGuests()).append(",");
                lines.append(r.getNights()).append(",");
                lines.append(formatDecimal(tax)).append("\n");
            }
        }
        return lines.toString();
    }

    @Override
    protected String buildTotalLine() {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);
        BigDecimal total = reservations.stream()
            .filter(Reservation::isPaid)
            .map(this::calculateTax)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return "total," + formatDecimal(total);
    }

    private BigDecimal calculateTax(Reservation r) {
        return TAX_PER_GUEST_PER_NIGHT
            .multiply(new BigDecimal(r.getGuests()))
            .multiply(new BigDecimal(r.getNights()));
    }
}

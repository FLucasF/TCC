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

public class TourismTaxReport extends ReportGenerator {
    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final ReservationRepository reservationRepository;
    private final int year;
    private final int month;
    private List<Reservation> reservations;

    public TourismTaxReport(ReservationRepository reservationRepository, int year, int month) {
        this.reservationRepository = reservationRepository;
        this.year = year;
        this.month = month;
    }

    @Override
    protected String getReportName() {
        return "tourism-tax";
    }

    @Override
    protected List<String> getHeaders() {
        return Arrays.asList("reservation", "room", "check_in", "check_out", "guests", "nights", "tax");
    }

    @Override
    protected List<String> getDataLines() {
        try {
            reservations = reservationRepository.findByCheckInMonth(year, month);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return reservations.stream()
                .filter(Reservation::isPaid)
                .map(r -> {
                    BigDecimal tax = calculateTax(r);
                    return String.format("%d,%s,%s,%s,%d,%d,%s",
                            r.getId(),
                            r.getRoomNumber(),
                            DateUtils.formatIso(r.getCheckIn()),
                            DateUtils.formatIso(r.getCheckOut()),
                            r.getGuests(),
                            r.getNights(),
                            MoneyUtils.formatCsv(tax));
                })
                .collect(Collectors.toList());
    }

    @Override
    protected String getTotalLine() {
        BigDecimal total = reservations.stream()
                .filter(Reservation::isPaid)
                .map(this::calculateTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return "total," + MoneyUtils.formatCsv(total);
    }

    private BigDecimal calculateTax(Reservation reservation) {
        int guestNights = reservation.getGuests() * reservation.getNights();
        return TAX_PER_GUEST_PER_NIGHT.multiply(new BigDecimal(guestNights));
    }
}

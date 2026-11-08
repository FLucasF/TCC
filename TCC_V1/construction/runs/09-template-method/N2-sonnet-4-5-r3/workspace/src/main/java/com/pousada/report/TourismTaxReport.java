package com.pousada.report;

import com.pousada.domain.Reservation;
import com.pousada.repository.ReservationRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TourismTaxReport extends Report {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final int year;
    private final int month;
    private final ReservationRepository reservationRepository;
    private List<Reservation> paidReservations;
    private BigDecimal totalTax;

    public TourismTaxReport(int year, int month, ReservationRepository reservationRepository) {
        this.year = year;
        this.month = month;
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String getReportName() {
        return "tourism-tax";
    }

    @Override
    protected String getPeriod() {
        return String.format("%04d-%02d", year, month);
    }

    @Override
    protected String buildHeaderLine() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected List<String> buildDataLines() {
        List<Reservation> allReservations = reservationRepository.findReservationsStartingInMonth(year, month);
        paidReservations = allReservations.stream()
            .filter(Reservation::isPaid)
            .toList();

        totalTax = BigDecimal.ZERO;

        return paidReservations.stream()
            .map(this::formatReservationLine)
            .toList();
    }

    @Override
    protected String buildTotalLine() {
        return String.format("total,%s", formatAmount(totalTax));
    }

    private String formatReservationLine(Reservation reservation) {
        BigDecimal tax = calculateTax(reservation);
        totalTax = totalTax.add(tax);

        return String.join(",",
            reservation.getId().toString(),
            reservation.getRoomNumber(),
            DATE_FORMATTER.format(reservation.getCheckIn()),
            DATE_FORMATTER.format(reservation.getCheckOut()),
            String.valueOf(reservation.getGuests()),
            String.valueOf(reservation.getNights()),
            formatAmount(tax)
        );
    }

    private BigDecimal calculateTax(Reservation reservation) {
        return TAX_PER_GUEST_PER_NIGHT
            .multiply(BigDecimal.valueOf(reservation.getGuests()))
            .multiply(BigDecimal.valueOf(reservation.getNights()))
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}

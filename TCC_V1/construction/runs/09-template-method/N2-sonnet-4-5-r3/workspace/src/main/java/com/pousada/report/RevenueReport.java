package com.pousada.report;

import com.pousada.domain.Reservation;
import com.pousada.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RevenueReport extends Report {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final int year;
    private final int month;
    private final ReservationRepository reservationRepository;
    private List<Reservation> reservations;
    private BigDecimal totalRevenue;

    public RevenueReport(int year, int month, ReservationRepository reservationRepository) {
        this.year = year;
        this.month = month;
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String getReportName() {
        return "revenue";
    }

    @Override
    protected String getPeriod() {
        return String.format("%04d-%02d", year, month);
    }

    @Override
    protected String buildHeaderLine() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    protected List<String> buildDataLines() {
        reservations = reservationRepository.findReservationsStartingInMonth(year, month);
        totalRevenue = BigDecimal.ZERO;

        return reservations.stream()
            .map(this::formatReservationLine)
            .toList();
    }

    @Override
    protected String buildTotalLine() {
        return String.format("total,%s", formatAmount(totalRevenue));
    }

    private String formatReservationLine(Reservation reservation) {
        BigDecimal amount = calculateRevenue(reservation);
        totalRevenue = totalRevenue.add(amount);

        return String.join(",",
            reservation.getId().toString(),
            reservation.getRoomNumber(),
            DATE_FORMATTER.format(reservation.getCheckIn()),
            DATE_FORMATTER.format(reservation.getCheckOut()),
            reservation.getStatus().name(),
            formatAmount(amount)
        );
    }

    private BigDecimal calculateRevenue(Reservation reservation) {
        BigDecimal paid = reservation.getAmountPaid();
        BigDecimal refunded = reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refunded);
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}

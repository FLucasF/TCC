package com.pousada.report;

import com.pousada.domain.Reservation;
import com.pousada.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class RevenueReport extends ReportGenerator {
    private final ReservationRepository reservationRepository;

    public RevenueReport(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String getReportName() {
        return "revenue";
    }

    @Override
    protected String generateHeader() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    protected List<String> generateDataLines(String period) {
        YearMonth month = YearMonth.parse(period);
        List<Reservation> reservations = reservationRepository.findRevenueReservationsForMonth(month);

        List<String> lines = new ArrayList<>();
        for (Reservation reservation : reservations) {
            BigDecimal amount = reservation.getAmountPaid().subtract(
                reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO
            );

            lines.add(String.format("%d,%d,%s,%s,%s,%s",
                reservation.getId(),
                reservation.getRoomNumber(),
                formatDate(reservation.getCheckIn()),
                formatDate(reservation.getCheckOut()),
                reservation.getStatus(),
                formatMoney(amount)));
        }
        return lines;
    }

    @Override
    protected String generateTotal(String period) {
        YearMonth month = YearMonth.parse(period);
        List<Reservation> reservations = reservationRepository.findRevenueReservationsForMonth(month);

        BigDecimal total = reservations.stream()
            .map(r -> r.getAmountPaid().subtract(
                r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO
            ))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return String.format("total,%s", formatMoney(total));
    }
}

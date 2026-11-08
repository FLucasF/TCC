package com.pousada.report;

import com.pousada.domain.Reservation;
import com.pousada.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class TourismTaxReport extends ReportGenerator {
    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final ReservationRepository reservationRepository;

    public TourismTaxReport(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String getReportName() {
        return "tourism-tax";
    }

    @Override
    protected String generateHeader() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected List<String> generateDataLines(String period) {
        YearMonth month = YearMonth.parse(period);
        List<Reservation> reservations = reservationRepository.findPaidReservationsStartingInMonth(month);

        List<String> lines = new ArrayList<>();
        for (Reservation reservation : reservations) {
            BigDecimal tax = calculateTax(reservation);

            lines.add(String.format("%d,%d,%s,%s,%d,%d,%s",
                reservation.getId(),
                reservation.getRoomNumber(),
                formatDate(reservation.getCheckIn()),
                formatDate(reservation.getCheckOut()),
                reservation.getGuests(),
                reservation.getNights(),
                formatMoney(tax)));
        }
        return lines;
    }

    @Override
    protected String generateTotal(String period) {
        YearMonth month = YearMonth.parse(period);
        List<Reservation> reservations = reservationRepository.findPaidReservationsStartingInMonth(month);

        BigDecimal total = reservations.stream()
            .map(this::calculateTax)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return String.format("total,%s", formatMoney(total));
    }

    private BigDecimal calculateTax(Reservation reservation) {
        return TAX_PER_GUEST_PER_NIGHT
            .multiply(new BigDecimal(reservation.getGuests()))
            .multiply(new BigDecimal(reservation.getNights()));
    }
}

package com.pousadamarealta.report;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TourismTaxReport extends CsvReport {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final YearMonth month;
    private final ReservationRepository reservationRepository;
    private List<Reservation> reservations;

    public TourismTaxReport(YearMonth month, ReservationRepository reservationRepository) {
        this.month = month;
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String reportName() {
        return "tourism-tax";
    }

    @Override
    protected String period() {
        return month.format(MONTH_FORMATTER);
    }

    @Override
    protected String headerLine() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected List<String> dataLines() {
        reservations = reservationRepository.findPaidByCheckInMonth(month);

        List<String> lines = new ArrayList<>();
        for (Reservation r : reservations) {
            BigDecimal tax = calculateTax(r);
            lines.add(String.format("%d,%s,%s,%s,%d,%d,%s",
                r.getId(),
                r.getRoomNumber(),
                r.getCheckIn().format(DATE_FORMATTER),
                r.getCheckOut().format(DATE_FORMATTER),
                r.getGuests(),
                r.getNights(),
                formatAmount(tax)
            ));
        }

        return lines;
    }

    @Override
    protected String totalLine() {
        BigDecimal total = reservations.stream()
                .map(this::calculateTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, java.math.RoundingMode.HALF_EVEN);
        return "total," + formatAmount(total);
    }

    private BigDecimal calculateTax(Reservation reservation) {
        return TAX_PER_GUEST_PER_NIGHT
                .multiply(new BigDecimal(reservation.getGuests()))
                .multiply(new BigDecimal(reservation.getNights()))
                .setScale(2, java.math.RoundingMode.HALF_EVEN);
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_EVEN).toPlainString();
    }
}

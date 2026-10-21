package com.pousada.report;

import com.pousada.domain.Reservation;
import com.pousada.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TourismTaxReport extends Report {
    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final YearMonth month;
    private final ReservationRepository reservationRepository;

    public TourismTaxReport(YearMonth month, ReservationRepository reservationRepository) {
        this.month = month;
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String getReportName() {
        return "tourism-tax";
    }

    @Override
    protected String getPeriod() {
        return month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    @Override
    protected String buildHeaderLine() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected List<String> buildDataLines() {
        List<String> lines = new ArrayList<>();
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);

        reservations.stream()
                .filter(Reservation::isPaid)
                .sorted(Comparator.comparing(Reservation::getCheckIn)
                        .thenComparing(Reservation::getId))
                .forEach(r -> {
                    BigDecimal tax = calculateTax(r);
                    lines.add(String.format("%d,%s,%s,%s,%d,%d,%s",
                            r.getId(),
                            r.getRoomNumber(),
                            r.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE),
                            r.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE),
                            r.getGuests(),
                            r.getNights(),
                            formatAmount(tax)
                    ));
                });

        return lines;
    }

    @Override
    protected String buildTotalLine() {
        BigDecimal total = reservationRepository.findByCheckInMonth(month).stream()
                .filter(Reservation::isPaid)
                .map(this::calculateTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return "total," + formatAmount(total);
    }

    private BigDecimal calculateTax(Reservation reservation) {
        return TAX_PER_GUEST_PER_NIGHT
                .multiply(new BigDecimal(reservation.getGuests()))
                .multiply(new BigDecimal(reservation.getNights()));
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}

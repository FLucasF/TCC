package com.pousadamarealta.report;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TourismTaxReport extends CsvReport {
    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final YearMonth month;
    private final ReservationRepository reservationRepository;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DecimalFormat numberFormatter;

    public TourismTaxReport(YearMonth month, ReservationRepository reservationRepository) {
        this.month = month;
        this.reservationRepository = reservationRepository;

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        symbols.setGroupingSeparator(',');
        this.numberFormatter = new DecimalFormat("0.00", symbols);
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
    protected List<String> getColumnNames() {
        return List.of("reservation", "room", "check_in", "check_out", "guests", "nights", "tax");
    }

    @Override
    protected List<List<String>> getDataRows() {
        List<Reservation> reservations = reservationRepository.findByMonth(month);
        List<List<String>> rows = new ArrayList<>();

        for (Reservation reservation : reservations) {
            if (reservation.isPaid()) {
                BigDecimal tax = calculateTax(reservation.getGuests(), reservation.getNights());
                rows.add(List.of(
                    String.valueOf(reservation.getId()),
                    String.valueOf(reservation.getRoomNumber()),
                    reservation.getCheckIn().format(dateFormatter),
                    reservation.getCheckOut().format(dateFormatter),
                    String.valueOf(reservation.getGuests()),
                    String.valueOf(reservation.getNights()),
                    numberFormatter.format(tax)
                ));
            }
        }

        return rows;
    }

    @Override
    protected List<String> getTotalRow() {
        List<Reservation> reservations = reservationRepository.findByMonth(month);
        BigDecimal total = reservations.stream()
            .filter(Reservation::isPaid)
            .map(r -> calculateTax(r.getGuests(), r.getNights()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return List.of("total", numberFormatter.format(total));
    }

    private BigDecimal calculateTax(int guests, int nights) {
        return TAX_PER_GUEST_PER_NIGHT
            .multiply(BigDecimal.valueOf(guests))
            .multiply(BigDecimal.valueOf(nights));
    }
}

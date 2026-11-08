package com.marealta.report;

import com.marealta.db.ReservationRepository;
import com.marealta.model.Reservation;
import com.marealta.util.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TourismTaxReport implements Report {
    private final ReservationRepository reservationRepository;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    public TourismTaxReport(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    public String generate(LocalDate parameter) {
        YearMonth month = YearMonth.from(parameter);
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth().plusDays(1);

        List<Reservation> reservations = reservationRepository.findByMonthAndStatuses(
            monthStart, monthEnd,
            List.of("CONFIRMED", "CHECKED_IN", "CHECKED_OUT")
        );

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,tourism-tax,").append(month).append("\n");
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation res : reservations) {
            BigDecimal tax = Money.multiply(TAX_PER_GUEST_PER_NIGHT, BigDecimal.valueOf(res.getGuests() * res.getNights()));
            csv.append(res.getId()).append(",")
                .append(res.getRoomNumber()).append(",")
                .append(res.getCheckIn().format(DATE_FORMAT)).append(",")
                .append(res.getCheckOut().format(DATE_FORMAT)).append(",")
                .append(res.getGuests()).append(",")
                .append(res.getNights()).append(",")
                .append(formatMoney(tax)).append("\n");
            total = Money.add(total, tax);
        }

        csv.append("total,,,,,").append(formatMoney(total));

        return csv.toString();
    }

    private String formatMoney(BigDecimal value) {
        return String.format("%.2f", value);
    }
}

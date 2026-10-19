package com.pousadamarealta.service.report;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TourismTaxReportGenerator implements ReportGenerator {
    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    @Override
    public String generate(List<Reservation> reservations, List<Room> rooms, String monthStr) {
        YearMonth month = YearMonth.parse(monthStr);

        List<Reservation> monthReservations = reservations.stream()
            .filter(Reservation::isPaid)
            .filter(r -> {
                YearMonth checkInMonth = YearMonth.from(r.getCheckIn());
                return checkInMonth.equals(month);
            })
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                             .thenComparing(Reservation::getId))
            .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,tourism-tax,").append(monthStr).append("\n");
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        BigDecimal totalTax = BigDecimal.ZERO;
        for (Reservation reservation : monthReservations) {
            BigDecimal tax = TAX_PER_GUEST_PER_NIGHT
                .multiply(BigDecimal.valueOf(reservation.getGuests()))
                .multiply(BigDecimal.valueOf(reservation.getNights()));

            totalTax = totalTax.add(tax);

            csv.append(reservation.getId()).append(",")
               .append(reservation.getRoomNumber()).append(",")
               .append(reservation.getCheckIn()).append(",")
               .append(reservation.getCheckOut()).append(",")
               .append(reservation.getGuests()).append(",")
               .append(reservation.getNights()).append(",")
               .append(formatMoney(tax)).append("\n");
        }

        csv.append("total,").append(formatMoney(totalTax));

        return csv.toString();
    }

    private String formatMoney(BigDecimal amount) {
        return amount.toPlainString();
    }
}

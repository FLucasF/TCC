package com.pousadamarealta.service.report;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RevenueReportGenerator implements ReportGenerator {
    @Override
    public String generate(List<Reservation> reservations, List<Room> rooms, String monthStr) {
        YearMonth month = YearMonth.parse(monthStr);

        List<Reservation> monthReservations = reservations.stream()
            .filter(r -> {
                YearMonth checkInMonth = YearMonth.from(r.getCheckIn());
                return checkInMonth.equals(month);
            })
            .filter(r -> r.getNetRevenue().compareTo(BigDecimal.ZERO) > 0)
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                             .thenComparing(Reservation::getId))
            .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,revenue,").append(monthStr).append("\n");
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation reservation : monthReservations) {
            BigDecimal amount = reservation.getNetRevenue();
            total = total.add(amount);

            csv.append(reservation.getId()).append(",")
               .append(reservation.getRoomNumber()).append(",")
               .append(reservation.getCheckIn()).append(",")
               .append(reservation.getCheckOut()).append(",")
               .append(reservation.getStatus()).append(",")
               .append(formatMoney(amount)).append("\n");
        }

        csv.append("total,").append(formatMoney(total));

        return csv.toString();
    }

    private String formatMoney(BigDecimal amount) {
        return amount.toPlainString();
    }
}

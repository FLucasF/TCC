package com.pousada.report.generators;

import com.pousada.domain.model.Reservation;
import com.pousada.infrastructure.repository.RoomRepository;
import com.pousada.report.ReportGenerator;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class RevenueReportGenerator implements ReportGenerator {

    @Override
    public String getName() {
        return "revenue";
    }

    @Override
    public String generate(String period, List<Reservation> reservations, RoomRepository roomRepository) {
        YearMonth month = YearMonth.parse(period);

        List<Reservation> monthReservations = reservations.stream()
            .filter(r -> {
                YearMonth checkInMonth = YearMonth.from(r.getCheckIn());
                return checkInMonth.equals(month);
            })
            .filter(r -> r.getNetAmount().compareTo(BigDecimal.ZERO) > 0)
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                             .thenComparing(Reservation::getId))
            .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,").append(getName()).append(",").append(period).append("\n");
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation reservation : monthReservations) {
            BigDecimal amount = reservation.getNetAmount();
            csv.append(reservation.getId()).append(",")
               .append(reservation.getRoomNumber()).append(",")
               .append(reservation.getCheckIn()).append(",")
               .append(reservation.getCheckOut()).append(",")
               .append(reservation.getStatus()).append(",")
               .append(formatDecimal(amount)).append("\n");
            total = total.add(amount);
        }

        csv.append("total,").append(formatDecimal(total));

        return csv.toString();
    }

    private String formatDecimal(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}

package com.pousada.strategy;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class RevenueReportStrategy implements ReportStrategy {
    @Override
    public String generate(List<Reservation> reservations, List<Room> rooms, Object parameter) {
        YearMonth month = (YearMonth) parameter;
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,revenue,").append(month.format(monthFormatter)).append("\n");
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalAmount = BigDecimal.ZERO;

        List<Reservation> monthReservations = reservations.stream()
                .filter(r -> YearMonth.from(r.getCheckIn()).equals(month))
                .filter(r -> getNetRevenue(r, rooms).compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(Reservation::getCheckIn)
                        .thenComparingLong(Reservation::getId))
                .toList();

        for (Reservation reservation : monthReservations) {
            BigDecimal amount = getNetRevenue(reservation, rooms);
            totalAmount = totalAmount.add(amount);

            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(reservation.getCheckIn().format(dateFormatter)).append(",");
            csv.append(reservation.getCheckOut().format(dateFormatter)).append(",");
            csv.append(reservation.getStatus()).append(",");
            csv.append(formatCsvNumber(amount)).append("\n");
        }

        csv.append("total,").append(formatCsvNumber(totalAmount));

        return csv.toString();
    }

    @Override
    public String getReportName() {
        return "revenue";
    }

    private BigDecimal getNetRevenue(Reservation reservation, List<Room> rooms) {
        BigDecimal paid = reservation.getAmountPaid();
        BigDecimal refunded = reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refunded);
    }

    private String formatCsvNumber(BigDecimal number) {
        return number.setScale(2).toString().replace(",", "");
    }
}

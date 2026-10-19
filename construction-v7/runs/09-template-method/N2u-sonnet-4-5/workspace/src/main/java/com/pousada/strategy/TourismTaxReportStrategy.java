package com.pousada.strategy;

import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.domain.Room;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class TourismTaxReportStrategy implements ReportStrategy {
    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    @Override
    public String generate(List<Reservation> reservations, List<Room> rooms, Object parameter) {
        YearMonth month = (YearMonth) parameter;
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,tourism-tax,").append(month.format(monthFormatter)).append("\n");
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        BigDecimal totalTax = BigDecimal.ZERO;

        List<Reservation> paidReservations = reservations.stream()
                .filter(r -> YearMonth.from(r.getCheckIn()).equals(month))
                .filter(r -> isPaid(r.getStatus()))
                .sorted(Comparator.comparing(Reservation::getCheckIn)
                        .thenComparingLong(Reservation::getId))
                .toList();

        for (Reservation reservation : paidReservations) {
            BigDecimal tax = calculateTax(reservation);
            totalTax = totalTax.add(tax);

            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(reservation.getCheckIn().format(dateFormatter)).append(",");
            csv.append(reservation.getCheckOut().format(dateFormatter)).append(",");
            csv.append(reservation.getGuests()).append(",");
            csv.append(reservation.getNights()).append(",");
            csv.append(formatCsvNumber(tax)).append("\n");
        }

        csv.append("total,").append(formatCsvNumber(totalTax));

        return csv.toString();
    }

    @Override
    public String getReportName() {
        return "tourism-tax";
    }

    private boolean isPaid(ReservationStatus status) {
        return status == ReservationStatus.CONFIRMED ||
               status == ReservationStatus.CHECKED_IN ||
               status == ReservationStatus.CHECKED_OUT;
    }

    private BigDecimal calculateTax(Reservation reservation) {
        int guestNights = reservation.getGuests() * reservation.getNights();
        return TAX_PER_GUEST_PER_NIGHT.multiply(new BigDecimal(guestNights));
    }

    private String formatCsvNumber(BigDecimal number) {
        return number.setScale(2).toString().replace(",", "");
    }
}

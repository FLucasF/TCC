package com.pousada.report.generators;

import com.pousada.domain.model.Reservation;
import com.pousada.domain.pricing.PricingCalculator;
import com.pousada.infrastructure.repository.RoomRepository;
import com.pousada.report.ReportGenerator;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class TourismTaxReportGenerator implements ReportGenerator {

    @Override
    public String getName() {
        return "tourism-tax";
    }

    @Override
    public String generate(String period, List<Reservation> reservations, RoomRepository roomRepository) {
        YearMonth month = YearMonth.parse(period);

        List<Reservation> paidReservations = reservations.stream()
            .filter(Reservation::isPaid)
            .filter(r -> {
                YearMonth checkInMonth = YearMonth.from(r.getCheckIn());
                return checkInMonth.equals(month);
            })
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                             .thenComparing(Reservation::getId))
            .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,").append(getName()).append(",").append(period).append("\n");
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        BigDecimal totalTax = BigDecimal.ZERO;
        for (Reservation reservation : paidReservations) {
            BigDecimal tax = PricingCalculator.calculateTourismTax(
                reservation.getGuests(),
                reservation.getNights()
            );

            csv.append(reservation.getId()).append(",")
               .append(reservation.getRoomNumber()).append(",")
               .append(reservation.getCheckIn()).append(",")
               .append(reservation.getCheckOut()).append(",")
               .append(reservation.getGuests()).append(",")
               .append(reservation.getNights()).append(",")
               .append(formatDecimal(tax)).append("\n");

            totalTax = totalTax.add(tax);
        }

        csv.append("total,").append(formatDecimal(totalTax));

        return csv.toString();
    }

    private String formatDecimal(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}

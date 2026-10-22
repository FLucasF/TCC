package com.pousada.reports;

import com.pousada.db.ReservationRepository;
import com.pousada.models.Reservation;
import com.pousada.utils.PricingUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class TourismTaxReport extends Report {
    private static final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final ReservationRepository reservationRepo;
    private final int month;
    private final int year;

    public TourismTaxReport(ReservationRepository reservationRepo, int month, int year) {
        this.reservationRepo = reservationRepo;
        this.month = month;
        this.year = year;
    }

    @Override
    public String generate() {
        try {
            StringBuilder sb = new StringBuilder();
            String period = String.format("%04d-%02d", year, month);
            String title = generateTitle("tourism-tax", period);
            sb.append(title).append("\n");

            sb.append(generateCsvRow("reservation", "room", "check_in", "check_out", "guests", "nights", "tax")).append("\n");

            List<Reservation> reservations = reservationRepo.findByMonthStarting(month, year);
            BigDecimal total = BigDecimal.ZERO;

            for (Reservation res : reservations) {
                BigDecimal tax = TAX_PER_GUEST_PER_NIGHT
                        .multiply(new BigDecimal(res.getGuests()))
                        .multiply(new BigDecimal(res.getNights()));
                tax = PricingUtils.roundToPair(tax);

                sb.append(generateCsvRow(
                        String.valueOf(res.getId()),
                        String.valueOf(res.getRoomNumber()),
                        PricingUtils.formatDateIso(res.getCheckIn()),
                        PricingUtils.formatDateIso(res.getCheckOut()),
                        String.valueOf(res.getGuests()),
                        String.valueOf(res.getNights()),
                        String.format("%.2f", tax)
                )).append("\n");

                total = total.add(tax);
            }

            sb.append(generateCsvRow("total", String.format("%.2f", total))).append("\n");

            return sb.toString();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

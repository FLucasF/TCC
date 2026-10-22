package com.pousada.reports;

import com.pousada.db.ReservationRepository;
import com.pousada.models.Reservation;
import com.pousada.utils.PricingUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class RevenueReport extends Report {
    private final ReservationRepository reservationRepo;
    private final int month;
    private final int year;

    public RevenueReport(ReservationRepository reservationRepo, int month, int year) {
        this.reservationRepo = reservationRepo;
        this.month = month;
        this.year = year;
    }

    @Override
    public String generate() {
        try {
            StringBuilder sb = new StringBuilder();
            String period = String.format("%04d-%02d", year, month);
            String title = generateTitle("revenue", period);
            sb.append(title).append("\n");

            sb.append(generateCsvRow("reservation", "room", "check_in", "check_out", "status", "amount")).append("\n");

            List<Reservation> reservations = reservationRepo.findByMonthStarting(month, year);
            BigDecimal total = BigDecimal.ZERO;

            for (Reservation res : reservations) {
                BigDecimal netAmount = res.getAmountPaid();
                if (res.getRefund() != null) {
                    netAmount = netAmount.subtract(res.getRefund());
                }

                sb.append(generateCsvRow(
                        String.valueOf(res.getId()),
                        String.valueOf(res.getRoomNumber()),
                        PricingUtils.formatDateIso(res.getCheckIn()),
                        PricingUtils.formatDateIso(res.getCheckOut()),
                        res.getStatus().getLabel(),
                        String.format("%.2f", netAmount)
                )).append("\n");

                total = total.add(netAmount);
            }

            sb.append(generateCsvRow("total", String.format("%.2f", total))).append("\n");

            return sb.toString();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

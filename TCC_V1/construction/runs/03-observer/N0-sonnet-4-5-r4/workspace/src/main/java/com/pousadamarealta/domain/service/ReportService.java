package com.pousadamarealta.domain.service;

import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final ReservationRepository reservationRepository;

    public ReportService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        try {
            List<Reservation> reservations = reservationRepository.findOccupiedOn(date);
            StringBuilder csv = new StringBuilder();
            csv.append("room,type,reservation,check_in,check_out\n");

            for (Reservation r : reservations) {
                csv.append(String.format("%s,%s,%d,%s,%s\n",
                    r.getRoomNumber(),
                    com.pousadamarealta.repository.RoomRepository.getRoomType(r.getRoomNumber()).name(),
                    r.getId(),
                    r.getCheckIn().format(DATE_FORMATTER),
                    r.getCheckOut().format(DATE_FORMATTER)
                ));
            }

            csv.append(String.format("total,%d/5", reservations.size()));
            return csv.toString();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to generate occupancy report", e);
        }
    }

    public String generateRevenueReport(int year, int month) {
        try {
            List<Reservation> reservations = reservationRepository.findRevenueForMonth(year, month);
            StringBuilder csv = new StringBuilder();
            csv.append("reservation,room,check_in,check_out,status,amount\n");

            BigDecimal total = BigDecimal.ZERO;

            for (Reservation r : reservations) {
                BigDecimal amount = r.getAmountPaid();
                if (r.getRefund() != null) {
                    amount = amount.subtract(r.getRefund());
                }

                csv.append(String.format("%d,%s,%s,%s,%s,%s\n",
                    r.getId(),
                    r.getRoomNumber(),
                    r.getCheckIn().format(DATE_FORMATTER),
                    r.getCheckOut().format(DATE_FORMATTER),
                    r.getStatus().name(),
                    formatAmount(amount)
                ));

                total = total.add(amount);
            }

            csv.append(String.format("total,%s", formatAmount(total)));
            return csv.toString();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to generate revenue report", e);
        }
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString().replace(',', '.');
    }
}

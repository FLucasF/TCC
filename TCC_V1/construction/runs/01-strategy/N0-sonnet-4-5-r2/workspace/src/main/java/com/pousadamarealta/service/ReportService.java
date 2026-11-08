package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;

    public ReportService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findOccupancyForDate(date);

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : reservations) {
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                r.getRoomNumber(),
                getRoomTypeName(r.getRoomNumber()),
                r.getId(),
                r.getCheckIn(),
                r.getCheckOut()));
        }

        csv.append(String.format("total,%d/5", reservations.size()));

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findRevenueForMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation r : reservations) {
            BigDecimal amount = r.getAmountPaid();
            if (r.getRefund() != null) {
                amount = amount.subtract(r.getRefund());
            }

            total = total.add(amount);

            csv.append(String.format("%d,%s,%s,%s,%s,%s\n",
                r.getId(),
                r.getRoomNumber(),
                r.getCheckIn(),
                r.getCheckOut(),
                r.getStatus(),
                formatAmount(amount)));
        }

        csv.append(String.format("total,%s", formatAmount(total)));

        return csv.toString();
    }

    private String getRoomTypeName(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "UNKNOWN";
        };
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString().replace(",", ".");
    }
}

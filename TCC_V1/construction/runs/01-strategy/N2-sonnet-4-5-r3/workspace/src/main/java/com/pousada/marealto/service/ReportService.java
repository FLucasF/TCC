package com.pousada.marealto.service;

import com.pousada.marealto.domain.Reservation;
import com.pousada.marealto.domain.ReservationStatus;
import com.pousada.marealto.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int TOTAL_ROOMS = 5;

    public ReportService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findByDate(date);

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(getRoomType(reservation.getRoomNumber())).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getCheckIn().format(DATE_FORMATTER)).append(",");
            csv.append(reservation.getCheckOut().format(DATE_FORMATTER)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(TOTAL_ROOMS);

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepository.findByMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal revenue = calculateRevenue(reservation);

            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(reservation.getId()).append(",");
                csv.append(reservation.getRoomNumber()).append(",");
                csv.append(reservation.getCheckIn().format(DATE_FORMATTER)).append(",");
                csv.append(reservation.getCheckOut().format(DATE_FORMATTER)).append(",");
                csv.append(reservation.getStatus().name()).append(",");
                csv.append(formatCsvDecimal(revenue)).append("\n");

                total = total.add(revenue);
            }
        }

        csv.append("total,").append(formatCsvDecimal(total));

        return csv.toString();
    }

    private BigDecimal calculateRevenue(Reservation reservation) {
        BigDecimal revenue = reservation.getAmountPaid();

        if (reservation.getRefund() != null) {
            revenue = revenue.subtract(reservation.getRefund());
        }

        return revenue;
    }

    private String formatCsvDecimal(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_EVEN)
                   .toString()
                   .replace(',', '.');
    }

    private String getRoomType(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "UNKNOWN";
        };
    }
}

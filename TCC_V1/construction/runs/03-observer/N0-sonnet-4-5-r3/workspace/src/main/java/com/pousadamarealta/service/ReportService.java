package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private static final DateTimeFormatter CSV_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReportService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findByDate(date);

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : reservations) {
            String roomType = getRoomType(r.getRoomNumber());
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                r.getRoomNumber(),
                roomType,
                r.getId(),
                r.getCheckIn().format(CSV_DATE_FORMATTER),
                r.getCheckOut().format(CSV_DATE_FORMATTER)
            ));
        }

        csv.append(String.format("total,%d/5", reservations.size()));

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepository.findByMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Reservation r : reservations) {
            BigDecimal revenue = r.getAmountPaid();

            if (r.getRefund() != null) {
                revenue = revenue.subtract(r.getRefund());
            }

            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(String.format(Locale.US, "%d,%s,%s,%s,%s,%.2f\n",
                    r.getId(),
                    r.getRoomNumber(),
                    r.getCheckIn().format(CSV_DATE_FORMATTER),
                    r.getCheckOut().format(CSV_DATE_FORMATTER),
                    r.getStatus(),
                    revenue
                ));

                totalRevenue = totalRevenue.add(revenue);
            }
        }

        csv.append(String.format(Locale.US, "total,%.2f", totalRevenue));

        return csv.toString();
    }

    private String getRoomType(String roomNumber) {
        if (roomNumber.startsWith("1")) return "STANDARD";
        if (roomNumber.startsWith("2")) return "DELUXE";
        return "SUITE";
    }
}

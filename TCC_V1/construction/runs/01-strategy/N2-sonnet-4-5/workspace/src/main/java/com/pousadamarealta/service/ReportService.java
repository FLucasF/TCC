package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.ReservationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;

    public ReportService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findByNight(date);

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                reservation.getRoomNumber(),
                reservation.getRatePlanType().name(),
                reservation.getId(),
                reservation.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE),
                reservation.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE)
            ));
        }

        csv.append(String.format("total,%d/5", reservations.size()));

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal netRevenue = reservation.getNetRevenue();
            total = total.add(netRevenue);

            csv.append(String.format("%d,%s,%s,%s,%s,%s\n",
                reservation.getId(),
                reservation.getRoomNumber(),
                reservation.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE),
                reservation.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE),
                reservation.getStatus().name(),
                formatCsvDecimal(netRevenue)
            ));
        }

        csv.append(String.format("total,%s", formatCsvDecimal(total)));

        return csv.toString();
    }

    private String formatCsvDecimal(BigDecimal value) {
        return value.toPlainString();
    }
}

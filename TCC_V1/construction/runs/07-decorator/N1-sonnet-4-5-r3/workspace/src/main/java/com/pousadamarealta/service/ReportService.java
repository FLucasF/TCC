package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findPaidReservationsForNight(date);
        int totalRooms = roomRepository.findAll().size();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : reservations) {
            csv.append(r.getRoomNumber()).append(",")
               .append(r.getRoomType()).append(",")
               .append(r.getId()).append(",")
               .append(r.getCheckIn().format(DATE_FORMATTER)).append(",")
               .append(r.getCheckOut().format(DATE_FORMATTER)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(totalRooms);

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepository.findRevenueForMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation r : reservations) {
            BigDecimal revenue = r.getNetRevenue();
            total = total.add(revenue);

            csv.append(r.getId()).append(",")
               .append(r.getRoomNumber()).append(",")
               .append(r.getCheckIn().format(DATE_FORMATTER)).append(",")
               .append(r.getCheckOut().format(DATE_FORMATTER)).append(",")
               .append(r.getStatus()).append(",")
               .append(formatAmount(revenue)).append("\n");
        }

        csv.append("total,").append(formatAmount(total));

        return csv.toString();
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}

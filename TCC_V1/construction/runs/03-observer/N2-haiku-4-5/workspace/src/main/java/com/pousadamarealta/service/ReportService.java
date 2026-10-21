package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.repository.ReservationRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class ReportService {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public ReportService(RoomRepository roomRepository, ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedRooms = 0;
        for (Room room : roomRepository.findAll()) {
            boolean occupied = false;
            for (Reservation reservation : reservationRepository.findByRoomAndDate(room.getNumber(), date)) {
                if (occupied) continue;
                occupied = true;
                occupiedRooms++;
                csv.append(room.getNumber()).append(",")
                   .append(room.getType().getName()).append(",")
                   .append(reservation.getId()).append(",")
                   .append(reservation.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(",")
                   .append(reservation.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n");
            }
        }

        csv.append("total,").append(occupiedRooms).append("/5\n");
        return csv.toString();
    }

    public String generateRevenueReport(YearMonth yearMonth) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
        for (Reservation reservation : reservationRepository.findByMonthStarting(yearMonth.getYear(), yearMonth.getMonthValue())) {
            java.math.BigDecimal amount = reservation.getAmountPaid().subtract(reservation.getRefund() == null ? java.math.BigDecimal.ZERO : reservation.getRefund());
            totalAmount = totalAmount.add(amount);

            csv.append(reservation.getId()).append(",")
               .append(reservation.getRoomNumber()).append(",")
               .append(reservation.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(",")
               .append(reservation.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(",")
               .append(reservation.getStatus()).append(",")
               .append(String.format("%.2f", amount)).append("\n");
        }

        csv.append("total,,,,").append(String.format("%.2f", totalAmount)).append("\n");
        return csv.toString();
    }
}

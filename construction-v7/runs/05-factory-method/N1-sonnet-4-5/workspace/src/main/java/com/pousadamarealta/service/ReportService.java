package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> occupiedReservations = reservationRepository.findAll().stream()
            .filter(r -> r.isPaid())
            .filter(r -> r.occupiesNight(date))
            .sorted(Comparator.comparing(Reservation::getRoomNumber))
            .toList();

        for (Reservation reservation : occupiedReservations) {
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
            csv.append(room.getNumber()).append(",");
            csv.append(room.getType()).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(",");
            csv.append(reservation.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n");
        }

        int totalRooms = roomRepository.findAll().size();
        csv.append("total,").append(occupiedReservations.size()).append("/").append(totalRooms);

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> monthReservations = reservationRepository.findAll().stream()
            .filter(r -> YearMonth.from(r.getCheckIn()).equals(month))
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                .thenComparing(Reservation::getId))
            .toList();

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Reservation reservation : monthReservations) {
            BigDecimal revenue = calculateRevenue(reservation);
            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(reservation.getId()).append(",");
                csv.append(reservation.getRoomNumber()).append(",");
                csv.append(reservation.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(",");
                csv.append(reservation.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(",");
                csv.append(reservation.getStatus()).append(",");
                csv.append(String.format("%.2f", revenue)).append("\n");
                totalRevenue = totalRevenue.add(revenue);
            }
        }

        csv.append("total,").append(String.format("%.2f", totalRevenue));

        return csv.toString();
    }

    private BigDecimal calculateRevenue(Reservation reservation) {
        BigDecimal paid = reservation.getAmountPaid();
        BigDecimal refunded = reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refunded);
    }
}

package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        List<Reservation> occupiedReservations = reservationRepository.findAll().stream()
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                        r.getStatus() == ReservationStatus.CHECKED_IN ||
                        r.getStatus() == ReservationStatus.CHECKED_OUT)
            .filter(r -> !r.getCheckIn().isAfter(date) && r.getCheckOut().isAfter(date))
            .sorted(Comparator.comparing(Reservation::getRoomNumber))
            .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : occupiedReservations) {
            csv.append(r.getRoomNumber()).append(",")
               .append(r.getRoomType()).append(",")
               .append(r.getId()).append(",")
               .append(r.getCheckIn()).append(",")
               .append(r.getCheckOut()).append("\n");
        }

        int totalRooms = roomRepository.findAll().size();
        csv.append("total,").append(occupiedReservations.size()).append("/").append(totalRooms);

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> monthReservations = reservationRepository.findAll().stream()
            .filter(r -> r.getCheckIn().getYear() == year && r.getCheckIn().getMonthValue() == month)
            .filter(r -> r.getStatus() != ReservationStatus.PENDING)
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                             .thenComparing(Reservation::getId))
            .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation r : monthReservations) {
            BigDecimal amount = r.getAmountPaid().subtract(
                r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO
            );
            total = total.add(amount);

            csv.append(r.getId()).append(",")
               .append(r.getRoomNumber()).append(",")
               .append(r.getCheckIn()).append(",")
               .append(r.getCheckOut()).append(",")
               .append(r.getStatus()).append(",")
               .append(String.format("%.2f", amount)).append("\n");
        }

        csv.append("total,").append(String.format("%.2f", total));

        return csv.toString();
    }
}

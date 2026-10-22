package com.pousada.marealta.service;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.Room;
import com.pousada.marealta.repository.ReservationRepository;
import com.pousada.marealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findByDateAndStatus(date, Reservation.Status.CONFIRMED);
        List<Reservation> checkedIn = reservationRepository.findByDateAndStatus(date, Reservation.Status.CHECKED_IN);
        List<Reservation> checkedOut = reservationRepository.findByDateAndStatus(date, Reservation.Status.CHECKED_OUT);

        reservations.addAll(checkedIn);
        reservations.addAll(checkedOut);

        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Room room : allRooms) {
            for (Reservation res : reservations) {
                if (res.getRoomNumber() == room.getNumber()) {
                    csv.append(room.getNumber()).append(",")
                        .append(room.getType()).append(",")
                        .append(res.getId()).append(",")
                        .append(res.getCheckIn()).append(",")
                        .append(res.getCheckOut()).append("\n");
                }
            }
        }

        long occupiedRooms = reservations.stream()
            .map(Reservation::getRoomNumber)
            .distinct()
            .count();
        csv.append("total,").append(occupiedRooms).append("/").append(allRooms.size()).append("\n");

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth yearMonth) {
        List<Reservation> reservations = reservationRepository.findByMonth(yearMonth.getYear(), yearMonth.getMonthValue());

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation res : reservations) {
            BigDecimal amount = res.getAmountPaid();
            if (res.getRefund() != null) {
                amount = amount.subtract(res.getRefund());
            }

            csv.append(res.getId()).append(",")
                .append(res.getRoomNumber()).append(",")
                .append(res.getCheckIn()).append(",")
                .append(res.getCheckOut()).append(",")
                .append(res.getStatus()).append(",")
                .append(String.format("%.2f", amount)).append("\n");

            total = total.add(amount);
        }

        csv.append("total,,,,").append(String.format("%.2f", total)).append("\n");

        return csv.toString();
    }
}

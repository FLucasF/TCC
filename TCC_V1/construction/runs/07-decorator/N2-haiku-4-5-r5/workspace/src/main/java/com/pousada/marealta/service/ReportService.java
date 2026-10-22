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

    public String generateOccupancyReport(LocalDate date) throws Exception {
        List<Reservation> reservations = reservationRepository.getConfirmedReservationsByDate(date);
        List<Room> rooms = roomRepository.getAllRooms();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedRooms = 0;
        for (Room room : rooms) {
            boolean occupied = false;
            for (Reservation res : reservations) {
                if (res.getRoomNumber() == room.getNumber()) {
                    csv.append(room.getNumber()).append(",")
                       .append(room.getType().getName()).append(",")
                       .append(res.getId()).append(",")
                       .append(formatDate(res.getCheckIn())).append(",")
                       .append(formatDate(res.getCheckOut())).append("\n");
                    occupied = true;
                    break;
                }
            }
            if (occupied) {
                occupiedRooms++;
            }
        }

        csv.append("total,").append(occupiedRooms).append("/").append(rooms.size());
        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) throws Exception {
        List<Reservation> reservations = reservationRepository.getReservationsByMonthAndStatus(year, month);
        List<Room> rooms = roomRepository.getAllRooms();

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation res : reservations) {
            BigDecimal amount = calculateRevenue(res);
            total = total.add(amount);

            csv.append(res.getId()).append(",")
               .append(res.getRoomNumber()).append(",")
               .append(formatDateDash(res.getCheckIn())).append(",")
               .append(formatDateDash(res.getCheckOut())).append(",")
               .append(res.getStatus().name()).append(",")
               .append(formatMoneyDash(amount)).append("\n");
        }

        csv.append("total,,,,").append(formatMoneyDash(total));
        return csv.toString();
    }

    private BigDecimal calculateRevenue(Reservation reservation) {
        if (reservation.getAmountPaid() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal paid = reservation.getAmountPaid();
        BigDecimal refund = reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refund);
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatDateDash(LocalDate date) {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private String formatMoneyDash(BigDecimal value) {
        return String.format("%.2f", value);
    }
}

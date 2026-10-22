package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.domain.Room;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class ReportService {
    private static final ReportService instance = new ReportService();
    private final ReservationRepository reservationRepo = ReservationRepository.getInstance();
    private final RoomRepository roomRepo = RoomRepository.getInstance();

    private ReportService() {
    }

    public static ReportService getInstance() {
        return instance;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Room> rooms = roomRepo.findAll();
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedCount = 0;
        for (Room room : rooms) {
            var reservations = reservationRepo.findByDateAndRoom(date, room.number());
            if (reservations.isEmpty()) {
                continue;
            }
            for (Reservation res : reservations) {
                csv.append(room.number()).append(",");
                csv.append(room.type().name()).append(",");
                csv.append(res.getId()).append(",");
                csv.append(formatDate(res.getCheckIn())).append(",");
                csv.append(formatDate(res.getCheckOut())).append("\n");
                occupiedCount++;
            }
        }

        csv.append("total,").append(occupiedCount).append("/").append(rooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepo.findByMonthAndStatus(year, month);
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation res : reservations) {
            BigDecimal amount = res.getAmountPaid();
            if (res.getRefund() != null) {
                amount = amount.subtract(res.getRefund());
            }

            csv.append(res.getId()).append(",");
            csv.append(res.getRoomNumber()).append(",");
            csv.append(formatDate(res.getCheckIn())).append(",");
            csv.append(formatDate(res.getCheckOut())).append(",");
            csv.append(res.getStatus().name()).append(",");
            csv.append(formatCurrency(amount)).append("\n");

            total = total.add(amount);
        }

        csv.append("total,,,,,").append(formatCurrency(total));

        return csv.toString();
    }

    private String formatDate(LocalDate date) {
        return date.toString();
    }

    private String formatCurrency(BigDecimal value) {
        return String.format("%.2f", value).replace(",", ".");
    }
}

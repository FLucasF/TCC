package com.pousada.service;

import com.pousada.dao.ReservationDAO;
import com.pousada.dao.RoomDAO;
import com.pousada.model.Reservation;
import com.pousada.model.Room;
import com.pousada.util.DocumentFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private static final ReservationDAO reservationDAO = new ReservationDAO();
    private static final RoomDAO roomDAO = new RoomDAO();

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> occupiedReservations = reservationDAO.findPaidByDate(date);
        List<Room> allRooms = roomDAO.findAll();

        // Map rooms by number for quick lookup
        Map<Integer, Room> roomMap = allRooms.stream()
            .collect(Collectors.toMap(Room::getNumber, r -> r));

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Room room : allRooms) {
            for (Reservation res : occupiedReservations) {
                if (res.getRoomNumber() == room.getNumber()) {
                    csv.append(room.getNumber()).append(",")
                        .append(room.getType()).append(",")
                        .append(res.getId()).append(",")
                        .append(DocumentFormatter.formatISODate(res.getCheckIn())).append(",")
                        .append(DocumentFormatter.formatISODate(res.getCheckOut())).append("\n");
                }
            }
        }

        int occupiedCount = (int) allRooms.stream()
            .filter(room -> occupiedReservations.stream()
                .anyMatch(res -> res.getRoomNumber() == room.getNumber()))
            .count();

        csv.append("total,").append(occupiedCount).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationDAO.findByMonthAndStatus(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation res : reservations) {
            BigDecimal netAmount = res.getAmountPaid();
            if (res.getRefund() != null && res.getRefund().compareTo(BigDecimal.ZERO) > 0) {
                netAmount = netAmount.subtract(res.getRefund());
            }

            csv.append(res.getId()).append(",")
                .append(res.getRoomNumber()).append(",")
                .append(DocumentFormatter.formatISODate(res.getCheckIn())).append(",")
                .append(DocumentFormatter.formatISODate(res.getCheckOut())).append(",")
                .append(res.getStatus()).append(",")
                .append(String.format("%.2f", netAmount)).append("\n");

            total = total.add(netAmount);
        }

        csv.append("total,,,,").append(String.format("%.2f", total));

        return csv.toString();
    }
}

package com.pousadamarealta.service;

import com.pousadamarealta.database.ReservationDAO;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private final RoomService roomService;
    private final ReservationDAO reservationDAO;

    public ReportService(RoomService roomService, ReservationDAO reservationDAO) {
        this.roomService = roomService;
        this.reservationDAO = reservationDAO;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Room> allRooms = roomService.getAllRooms();
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedCount = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

        for (Room room : allRooms) {
            List<Reservation> reservations = reservationDAO.findByRoomAndDate(room.getNumber(), date);

            if (!reservations.isEmpty()) {
                Reservation reservation = reservations.get(0);
                csv.append(String.format("%s,%s,%d,%s,%s\n",
                    room.getNumber(),
                    room.getType().name(),
                    reservation.getId(),
                    reservation.getCheckIn().format(formatter),
                    reservation.getCheckOut().format(formatter)));
                occupiedCount++;
            }
        }

        csv.append(String.format("total,%d/%d", occupiedCount, allRooms.size()));

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationDAO.findByCheckInMonth(year, month);
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalRevenue = BigDecimal.ZERO;
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

        for (Reservation reservation : reservations) {
            BigDecimal netAmount = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                netAmount = netAmount.subtract(reservation.getRefund());
            }

            if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(String.format("%d,%s,%s,%s,%s,%s\n",
                    reservation.getId(),
                    reservation.getRoomNumber(),
                    reservation.getCheckIn().format(formatter),
                    reservation.getCheckOut().format(formatter),
                    reservation.getStatus().name(),
                    formatDecimal(netAmount)));

                totalRevenue = totalRevenue.add(netAmount);
            }
        }

        csv.append(String.format("total,%s", formatDecimal(totalRevenue)));

        return csv.toString();
    }

    private String formatDecimal(BigDecimal value) {
        return value.toString();
    }
}

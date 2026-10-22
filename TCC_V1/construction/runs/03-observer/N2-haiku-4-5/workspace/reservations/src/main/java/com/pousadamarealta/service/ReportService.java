package com.pousadamarealta.service;

import com.pousadamarealta.database.ReservationDAO;
import com.pousadamarealta.database.RoomDAO;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.domain.Room;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReportService {
    private static final ReportService INSTANCE = new ReportService();

    private ReportService() {
    }

    public static ReportService getInstance() {
        return INSTANCE;
    }

    public String generateOccupancyReport(LocalDate date) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Room> rooms = RoomDAO.getInstance().getAllRooms();
        int occupiedRooms = 0;

        for (Room room : rooms) {
            List<Reservation> reservations = ReservationDAO.getInstance()
                    .getReservationsByRoom(room.getNumber(), date, date.plusDays(1));

            if (!reservations.isEmpty()) {
                for (Reservation res : reservations) {
                    if (res.getStatus() == ReservationStatus.CONFIRMED ||
                        res.getStatus() == ReservationStatus.CHECKED_IN ||
                        res.getStatus() == ReservationStatus.CHECKED_OUT) {
                        csv.append(String.format("%d,%s,%d,%s,%s\n",
                                room.getNumber(),
                                room.getTypeCode(),
                                res.getId(),
                                formatDate(res.getCheckIn()),
                                formatDate(res.getCheckOut())));
                        occupiedRooms++;
                    }
                }
            }
        }

        csv.append(String.format("total,%d/%d\n", occupiedRooms, rooms.size()));
        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth().plusDays(1);

        List<Reservation> reservations = ReservationDAO.getInstance()
                .getReservationsByDateRange(monthStart, monthEnd);

        double total = 0;
        for (Reservation res : reservations) {
            double amount = calculateAmount(res);
            csv.append(String.format("%d,%d,%s,%s,%s,%.2f\n",
                    res.getId(),
                    res.getRoomNumber(),
                    formatDate(res.getCheckIn()),
                    formatDate(res.getCheckOut()),
                    res.getStatus().getCode(),
                    amount));
            total += amount;
        }

        csv.append(String.format("total,%.2f\n", total));
        return csv.toString();
    }

    private double calculateAmount(Reservation res) {
        if (res.getStatus() == ReservationStatus.CANCELLED) {
            double paid = res.getAmountPaid().toDouble();
            double refund = res.getRefund() != null ? res.getRefund().toDouble() : 0;
            return paid - refund;
        }
        return res.getAmountPaid().toDouble();
    }

    private String formatDate(LocalDate date) {
        return date.toString();
    }
}

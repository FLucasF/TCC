package com.pousada.marealta.service;

import com.pousada.marealta.db.ReservationRepository;
import com.pousada.marealta.db.RoomRepository;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.ReservationStatus;
import com.pousada.marealta.model.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReportService {
    public static String generateOccupancyReport(LocalDate date) throws Exception {
        List<Reservation> reservations = ReservationRepository.findByDateAndStatus(date, ReservationStatus.CONFIRMED);
        List<Reservation> checkedIn = ReservationRepository.findByDateAndStatus(date, ReservationStatus.CHECKED_IN);
        List<Reservation> checkedOut = ReservationRepository.findByDateAndStatus(date, ReservationStatus.CHECKED_OUT);

        reservations.addAll(checkedIn);
        reservations.addAll(checkedOut);

        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");

        int occupiedCount = 0;
        for (Reservation res : reservations) {
            Room room = RoomRepository.findByNumber(res.roomNumber);
            csv.append(res.roomNumber).append(",")
                    .append(room.type).append(",")
                    .append(res.id).append(",")
                    .append(formatDate(res.checkIn)).append(",")
                    .append(formatDate(res.checkOut)).append("\n");
            occupiedCount++;
        }

        List<Room> allRooms = RoomRepository.findAll();
        csv.append("total,").append(occupiedCount).append("/").append(allRooms.size());

        return csv.toString();
    }

    public static String generateRevenueReport(int year, int month) throws Exception {
        List<Reservation> reservations = ReservationRepository.findByMonthAndStatus(year, month, null);

        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal amount = res.amountPaid;
            if (res.status == ReservationStatus.CANCELLED && res.refund != null) {
                amount = res.amountPaid.subtract(res.refund);
            }

            csv.append(res.id).append(",")
                    .append(res.roomNumber).append(",")
                    .append(formatDate(res.checkIn)).append(",")
                    .append(formatDate(res.checkOut)).append(",")
                    .append(res.status).append(",")
                    .append(formatMoney(amount)).append("\n");

            totalAmount = totalAmount.add(amount);
        }

        csv.append("total,,,,").append(formatMoney(totalAmount));

        return csv.toString();
    }

    private static String formatDate(LocalDate date) {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private static String formatMoney(BigDecimal value) {
        return String.format("%.2f", value).replace(",", ".");
    }
}

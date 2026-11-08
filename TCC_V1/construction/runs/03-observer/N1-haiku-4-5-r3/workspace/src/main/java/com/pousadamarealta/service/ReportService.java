package com.pousadamarealta.service;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
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

    public String generateOccupancyReport(LocalDate date) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedCount = 0;
        for (int roomNumber : new int[]{101, 102, 201, 202, 301}) {
            List<Reservation> reservations = reservationRepository.findByRoomOnDate(roomNumber, date);
            for (Reservation reservation : reservations) {
                if (reservation.getStatus() == ReservationStatus.CONFIRMED ||
                    reservation.getStatus() == ReservationStatus.CHECKED_IN ||
                    reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
                    String type = getRoomType(roomNumber);
                    String checkInDate = formatDateISO(reservation.getCheckIn());
                    String checkOutDate = formatDateISO(reservation.getCheckOut());
                    csv.append(String.format("%d,%s,%d,%s,%s\n", roomNumber, type, reservation.getId(), checkInDate, checkOutDate));
                    occupiedCount++;
                }
            }
        }

        csv.append(String.format("total,%d/5\n", occupiedCount));
        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> reservations = reservationRepository.findByMonthAndStatus(year, month);
        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            String checkInDate = formatDateISO(reservation.getCheckIn());
            String checkOutDate = formatDateISO(reservation.getCheckOut());
            String status = reservation.getStatus().toString();
            BigDecimal amount = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                amount = amount.subtract(reservation.getRefund());
            }

            csv.append(String.format("%d,%d,%s,%s,%s,%s\n",
                    reservation.getId(),
                    reservation.getRoomNumber(),
                    checkInDate,
                    checkOutDate,
                    status,
                    formatBigDecimal(amount)));

            total = total.add(amount);
        }

        csv.append(String.format("total,%s\n", formatBigDecimal(total)));
        return csv.toString();
    }

    private String getRoomType(int roomNumber) {
        return switch (roomNumber) {
            case 101, 102 -> "STANDARD";
            case 201, 202 -> "DELUXE";
            case 301 -> "SUITE";
            default -> "UNKNOWN";
        };
    }

    private String formatDateISO(LocalDate date) {
        return date.toString();
    }

    private String formatBigDecimal(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_EVEN).toPlainString();
    }
}

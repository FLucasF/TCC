package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ReportService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Room> allRooms = roomRepository.findAll();
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedCount = 0;

        for (Room room : allRooms) {
            List<Reservation> reservations = reservationRepository.findByRoomAndDateRange(
                room.number(), date, date.plusDays(1));

            Reservation occupying = reservations.stream()
                .filter(r -> r.status() == ReservationStatus.CONFIRMED ||
                           r.status() == ReservationStatus.CHECKED_IN ||
                           r.status() == ReservationStatus.CHECKED_OUT)
                .filter(r -> !r.checkIn().isAfter(date) && r.checkOut().isAfter(date))
                .findFirst()
                .orElse(null);

            if (occupying != null) {
                csv.append(room.number()).append(",")
                   .append(room.type()).append(",")
                   .append(occupying.id()).append(",")
                   .append(occupying.checkIn().format(DATE_FORMATTER)).append(",")
                   .append(occupying.checkOut().format(DATE_FORMATTER)).append("\n");
                occupiedCount++;
            }
        }

        csv.append("total,").append(occupiedCount).append("/").append(allRooms.size()).append("\n");

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation r : reservations) {
            BigDecimal amount = r.amountPaid();
            if (r.refund() != null) {
                amount = amount.subtract(r.refund());
            }

            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(r.id()).append(",")
                   .append(r.roomNumber()).append(",")
                   .append(r.checkIn().format(DATE_FORMATTER)).append(",")
                   .append(r.checkOut().format(DATE_FORMATTER)).append(",")
                   .append(r.status()).append(",")
                   .append(formatAmount(amount)).append("\n");
                total = total.add(amount);
            }
        }

        csv.append("total,").append(formatAmount(total)).append("\n");

        return csv.toString();
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}

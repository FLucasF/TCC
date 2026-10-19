package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepo;
    private final RoomRepository roomRepo;

    public ReportService(ReservationRepository reservationRepo, RoomRepository roomRepo) {
        this.reservationRepo = reservationRepo;
        this.roomRepo = roomRepo;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepo.findAll().stream()
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                        r.getStatus() == ReservationStatus.CHECKED_IN ||
                        r.getStatus() == ReservationStatus.CHECKED_OUT)
            .filter(r -> isOccupiedOnDate(r, date))
            .sorted(Comparator.comparingInt(Reservation::getRoomNumber))
            .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : reservations) {
            String roomType = roomRepo.findByNumber(r.getRoomNumber())
                .map(room -> room.type().displayName)
                .orElse("UNKNOWN");

            csv.append(r.getRoomNumber()).append(",")
                .append(roomType).append(",")
                .append(r.getId()).append(",")
                .append(formatDateCsv(r.getCheckIn())).append(",")
                .append(formatDateCsv(r.getCheckOut())).append("\n");
        }

        int totalOccupied = reservations.size();
        int totalRooms = 5;
        csv.append("total,").append(totalOccupied).append("/").append(totalRooms);

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepo.findAll().stream()
            .filter(r -> {
                YearMonth resMonth = YearMonth.from(r.getCheckIn());
                return resMonth.equals(month);
            })
            .filter(r -> r.getAmountPaid().compareTo(BigDecimal.ZERO) > 0)
            .sorted(Comparator
                .comparing(Reservation::getCheckIn)
                .thenComparingLong(Reservation::getId))
            .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation r : reservations) {
            BigDecimal amount = r.getAmountPaid();
            if (r.getRefund() != null) {
                amount = amount.subtract(r.getRefund());
            }

            csv.append(r.getId()).append(",")
                .append(r.getRoomNumber()).append(",")
                .append(formatDateCsv(r.getCheckIn())).append(",")
                .append(formatDateCsv(r.getCheckOut())).append(",")
                .append(r.getStatus().name()).append(",")
                .append(String.format("%.2f", amount)).append("\n");

            total = total.add(amount);
        }

        csv.append("total,,,,").append(String.format("%.2f", total));

        return csv.toString();
    }

    private boolean isOccupiedOnDate(Reservation r, LocalDate date) {
        return !date.isBefore(r.getCheckIn()) && date.isBefore(r.getCheckOut());
    }

    private String formatDateCsv(LocalDate date) {
        return date.toString();
    }
}

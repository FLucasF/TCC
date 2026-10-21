package com.pousadamareaita.service;

import com.pousadamareaita.model.Reservation;
import com.pousadamareaita.model.ReservationStatus;
import com.pousadamareaita.model.Room;
import com.pousadamareaita.storage.ReservationRepository;
import com.pousadamareaita.storage.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Room> allRooms = new ArrayList<>(roomRepository.findAll());
        allRooms.sort((r1, r2) -> Integer.compare(r1.getNumber(), r2.getNumber()));

        int occupiedCount = 0;
        for (Room room : allRooms) {
            for (Reservation res : reservationRepository.findAll()) {
                if (res.getRoomNumber() != room.getNumber()) {
                    continue;
                }
                if (res.getStatus() == ReservationStatus.CANCELLED) {
                    continue;
                }
                if (res.getStatus() != ReservationStatus.CONFIRMED &&
                    res.getStatus() != ReservationStatus.CHECKED_IN &&
                    res.getStatus() != ReservationStatus.CHECKED_OUT) {
                    continue;
                }

                // Room is occupied on 'date' if: checkIn <= date < checkOut
                if (!res.getCheckIn().isAfter(date) && res.getCheckOut().isAfter(date)) {
                    csv.append(room.getNumber()).append(",");
                    csv.append(room.getType()).append(",");
                    csv.append(res.getId()).append(",");
                    csv.append(formatDate(res.getCheckIn())).append(",");
                    csv.append(formatDate(res.getCheckOut())).append("\n");
                    occupiedCount++;
                }
            }
        }

        csv.append("total,").append(occupiedCount).append("/5\n");
        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> reservations = new ArrayList<>(reservationRepository.findAll());
        reservations.sort((r1, r2) -> {
            int dateComp = r1.getCheckIn().compareTo(r2.getCheckIn());
            if (dateComp != 0) return dateComp;
            return Integer.compare(r1.getId(), r2.getId());
        });

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            // Only include reservations that start in this month
            if (!YearMonth.from(res.getCheckIn()).equals(month)) {
                continue;
            }

            // Only include reservations that generated money or will generate money
            if (res.getStatus() == ReservationStatus.PENDING || res.getStatus() == ReservationStatus.CANCELLED) {
                if (res.getStatus() == ReservationStatus.CANCELLED && res.getAmountPaid().compareTo(BigDecimal.ZERO) == 0) {
                    // Cancelled without payment, no money involved
                    continue;
                }
            }

            // Calculate net amount (paid - refunded)
            BigDecimal netAmount = res.getAmountPaid();
            if (res.getRefund() != null) {
                netAmount = netAmount.subtract(res.getRefund());
            }

            if (netAmount.compareTo(BigDecimal.ZERO) > 0 || res.getStatus() != ReservationStatus.CANCELLED) {
                csv.append(res.getId()).append(",");
                csv.append(res.getRoomNumber()).append(",");
                csv.append(formatDate(res.getCheckIn())).append(",");
                csv.append(formatDate(res.getCheckOut())).append(",");
                csv.append(res.getStatus()).append(",");
                csv.append(formatCurrencyForCSV(res.getAmountPaid())).append("\n");
                totalAmount = totalAmount.add(netAmount);
            }
        }

        csv.append("total,,,,").append(formatCurrencyForCSV(totalAmount)).append("\n");
        return csv.toString();
    }

    private String formatDate(LocalDate date) {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private String formatCurrencyForCSV(BigDecimal amount) {
        return String.format("%.2f", amount);
    }
}

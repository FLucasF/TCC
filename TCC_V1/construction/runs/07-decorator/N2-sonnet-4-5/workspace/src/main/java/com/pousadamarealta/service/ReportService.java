package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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
        List<Reservation> occupiedReservations = reservationRepository.findPaidByDate(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation res : occupiedReservations) {
            Room room = roomRepository.findByNumber(res.roomNumber())
                .orElseThrow(() -> new RuntimeException("Room not found"));

            csv.append(res.roomNumber()).append(",");
            csv.append(room.type().name()).append(",");
            csv.append(res.id()).append(",");
            csv.append(res.checkIn().format(DATE_FORMATTER)).append(",");
            csv.append(res.checkOut().format(DATE_FORMATTER)).append("\n");
        }

        csv.append("total,").append(occupiedReservations.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal revenue = calculateRevenue(res);

            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(res.id()).append(",");
                csv.append(res.roomNumber()).append(",");
                csv.append(res.checkIn().format(DATE_FORMATTER)).append(",");
                csv.append(res.checkOut().format(DATE_FORMATTER)).append(",");
                csv.append(res.status().name()).append(",");
                csv.append(formatDecimal(revenue)).append("\n");

                total = total.add(revenue);
            }
        }

        csv.append("total,").append(formatDecimal(total));

        return csv.toString();
    }

    private BigDecimal calculateRevenue(Reservation reservation) {
        BigDecimal paid = reservation.amountPaid() != null ? reservation.amountPaid() : BigDecimal.ZERO;
        BigDecimal refunded = reservation.refund() != null ? reservation.refund() : BigDecimal.ZERO;
        return paid.subtract(refunded);
    }

    private String formatDecimal(BigDecimal value) {
        return value.toPlainString();
    }
}

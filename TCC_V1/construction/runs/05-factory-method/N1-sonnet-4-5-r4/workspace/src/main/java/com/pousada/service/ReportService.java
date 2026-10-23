package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.infrastructure.ReservationRepository;
import com.pousada.infrastructure.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public ReportService(RoomRepository roomRepository, ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findByNight(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(getRoomType(reservation.getRoomNumber())).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(DATE_FORMAT.format(reservation.getCheckIn())).append(",");
            csv.append(DATE_FORMAT.format(reservation.getCheckOut())).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepository.findByMonthWithRevenue(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal revenue = reservation.getRevenueAmount();
            totalRevenue = totalRevenue.add(revenue);

            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(DATE_FORMAT.format(reservation.getCheckIn())).append(",");
            csv.append(DATE_FORMAT.format(reservation.getCheckOut())).append(",");
            csv.append(reservation.getStatus()).append(",");
            csv.append(formatMoney(revenue)).append("\n");
        }

        csv.append("total,").append(formatMoney(totalRevenue));

        return csv.toString();
    }

    private String getRoomType(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "";
        };
    }

    private String formatMoney(BigDecimal amount) {
        return String.format(Locale.US, "%.2f", amount);
    }
}

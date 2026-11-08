package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findPaidByNight(date);
        int totalRooms = roomRepository.findAll().size();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(getRoomType(reservation.getRoomNumber())).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getCheckIn().format(DATE_FORMAT)).append(",");
            csv.append(reservation.getCheckOut().format(DATE_FORMAT)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(totalRooms);

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(year, month);

        BigDecimal total = BigDecimal.ZERO;
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        for (Reservation reservation : reservations) {
            BigDecimal netRevenue = reservation.getNetRevenue();

            if (netRevenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(reservation.getId()).append(",");
                csv.append(reservation.getRoomNumber()).append(",");
                csv.append(reservation.getCheckIn().format(DATE_FORMAT)).append(",");
                csv.append(reservation.getCheckOut().format(DATE_FORMAT)).append(",");
                csv.append(reservation.getStatus()).append(",");
                csv.append(formatMoney(netRevenue)).append("\n");

                total = total.add(netRevenue);
            }
        }

        csv.append("total,").append(formatMoney(total));

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
        return amount.toString();
    }
}

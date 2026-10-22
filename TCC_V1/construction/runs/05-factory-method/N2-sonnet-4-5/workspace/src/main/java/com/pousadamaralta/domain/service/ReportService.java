package com.pousadamaralta.domain.service;

import com.pousadamaralta.domain.model.Reservation;
import com.pousadamaralta.domain.model.Room;
import com.pousadamaralta.infrastructure.repository.ReservationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationRepository reservationRepository;
    private final RoomService roomService;

    public ReportService(ReservationRepository reservationRepository, RoomService roomService) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findPaidReservationsForNight(date);
        List<Room> allRooms = roomService.getAllRooms();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            csv.append(reservation.getRoomNumber()).append(",");

            Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
            csv.append(room.getType().name()).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getCheckIn().format(DATE_FORMATTER)).append(",");
            csv.append(reservation.getCheckOut().format(DATE_FORMATTER)).append("\n");
        }

        int occupiedCount = reservations.size();
        int totalCount = allRooms.size();
        csv.append("total,").append(occupiedCount).append("/").append(totalCount);

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findRevenueReservationsForMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal revenue = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                revenue = revenue.subtract(reservation.getRefund());
            }

            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(reservation.getCheckIn().format(DATE_FORMATTER)).append(",");
            csv.append(reservation.getCheckOut().format(DATE_FORMATTER)).append(",");
            csv.append(reservation.getStatus().name()).append(",");
            csv.append(formatCsvAmount(revenue)).append("\n");

            totalRevenue = totalRevenue.add(revenue);
        }

        csv.append("total,").append(formatCsvAmount(totalRevenue));

        return csv.toString();
    }

    private String formatCsvAmount(BigDecimal amount) {
        return amount.toString();
    }
}

package com.pousadamareal.service;

import com.pousadamareal.model.Reservation;
import com.pousadamareal.model.Room;
import com.pousadamareal.repository.ReservationRepository;
import com.pousadamareal.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findByNight(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            csv.append(reservation.getRoomNumber()).append(",");

            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            csv.append(room.getType()).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getCheckIn().format(DATE_FORMATTER)).append(",");
            csv.append(reservation.getCheckOut().format(DATE_FORMATTER)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal revenue = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                revenue = revenue.subtract(reservation.getRefund());
            }

            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(reservation.getId()).append(",");
                csv.append(reservation.getRoomNumber()).append(",");
                csv.append(reservation.getCheckIn().format(DATE_FORMATTER)).append(",");
                csv.append(reservation.getCheckOut().format(DATE_FORMATTER)).append(",");
                csv.append(reservation.getStatus()).append(",");
                csv.append(formatAmount(revenue)).append("\n");

                totalRevenue = totalRevenue.add(revenue);
            }
        }

        csv.append("total,").append(formatAmount(totalRevenue));

        return csv.toString();
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}

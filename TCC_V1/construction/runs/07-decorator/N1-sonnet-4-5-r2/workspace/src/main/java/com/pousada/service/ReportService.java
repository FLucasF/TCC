package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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
        List<Reservation> reservations = reservationRepository.findByNightAndPaid(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            csv.append(reservation.getRoomNumber()).append(",")
               .append(room.getType().name()).append(",")
               .append(reservation.getId()).append(",")
               .append(reservation.getCheckIn().format(DATE_FORMAT)).append(",")
               .append(reservation.getCheckOut().format(DATE_FORMAT)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByMonthAndRevenue(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal revenue = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                revenue = revenue.subtract(reservation.getRefund());
            }

            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(reservation.getId()).append(",")
                   .append(reservation.getRoomNumber()).append(",")
                   .append(reservation.getCheckIn().format(DATE_FORMAT)).append(",")
                   .append(reservation.getCheckOut().format(DATE_FORMAT)).append(",")
                   .append(reservation.getStatus().name()).append(",")
                   .append(revenue.toString()).append("\n");

                total = total.add(revenue);
            }
        }

        csv.append("total,").append(total.toString());

        return csv.toString();
    }
}

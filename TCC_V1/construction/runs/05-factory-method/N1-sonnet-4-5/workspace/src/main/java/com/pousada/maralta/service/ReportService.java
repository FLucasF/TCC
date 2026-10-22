package com.pousada.maralta.service;

import com.pousada.maralta.domain.Reservation;
import com.pousada.maralta.domain.Room;
import com.pousada.maralta.repository.ReservationRepository;
import com.pousada.maralta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findPaidByNight(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

        for (Reservation reservation : reservations) {
            csv.append(reservation.getRoomNumber()).append(",");

            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            csv.append(room.type()).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getCheckIn().format(formatter)).append(",");
            csv.append(reservation.getCheckOut().format(formatter)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal revenue = reservation.getRevenue();
            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(reservation.getId()).append(",");
                csv.append(reservation.getRoomNumber()).append(",");
                csv.append(reservation.getCheckIn().format(formatter)).append(",");
                csv.append(reservation.getCheckOut().format(formatter)).append(",");
                csv.append(reservation.getStatus()).append(",");
                csv.append(String.format("%.2f", revenue)).append("\n");

                total = total.add(revenue);
            }
        }

        csv.append("total,").append(String.format("%.2f", total));

        return csv.toString();
    }
}

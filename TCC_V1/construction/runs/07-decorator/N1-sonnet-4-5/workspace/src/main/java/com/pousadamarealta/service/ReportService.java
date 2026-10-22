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
import java.util.Locale;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findPaidByDate(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(room.getType()).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(formatDateForCsv(reservation.getCheckIn())).append(",");
            csv.append(formatDateForCsv(reservation.getCheckOut())).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(formatDateForCsv(reservation.getCheckIn())).append(",");
            csv.append(formatDateForCsv(reservation.getCheckOut())).append(",");
            csv.append(reservation.getStatus()).append(",");
            csv.append(formatMoneyForCsv(reservation.getRevenue())).append("\n");

            total = total.add(reservation.getRevenue());
        }

        csv.append("total,").append(formatMoneyForCsv(total));

        return csv.toString();
    }

    private String formatDateForCsv(LocalDate date) {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String formatMoneyForCsv(BigDecimal amount) {
        return String.format(Locale.US, "%.2f", amount);
    }
}

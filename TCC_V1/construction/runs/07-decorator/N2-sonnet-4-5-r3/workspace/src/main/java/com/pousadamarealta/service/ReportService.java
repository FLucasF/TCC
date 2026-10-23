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

    public String generateOccupancyReport(LocalDate night) {
        List<Reservation> reservations = reservationRepository.findByNight(night);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            csv.append(reservation.getRoomNumber()).append(",");

            Room room = allRooms.stream()
                .filter(r -> r.number().equals(reservation.getRoomNumber()))
                .findFirst()
                .orElseThrow();

            csv.append(room.type()).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getCheckIn().format(DATE_FORMATTER)).append(",");
            csv.append(reservation.getCheckOut().format(DATE_FORMATTER)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal retained = reservation.getAmountRetained();
            if (retained.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(reservation.getId()).append(",");
                csv.append(reservation.getRoomNumber()).append(",");
                csv.append(reservation.getCheckIn().format(DATE_FORMATTER)).append(",");
                csv.append(reservation.getCheckOut().format(DATE_FORMATTER)).append(",");
                csv.append(reservation.getStatus()).append(",");
                csv.append(formatDecimal(retained)).append("\n");

                totalRevenue = totalRevenue.add(retained);
            }
        }

        csv.append("total,").append(formatDecimal(totalRevenue));

        return csv.toString();
    }

    private String formatDecimal(BigDecimal value) {
        return value.toString().replace(",", ".");
    }
}

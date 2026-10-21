package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> occupiedReservations = reservationRepository.findAll().stream()
            .filter(r -> r.isPaid())
            .filter(r -> r.occupiesNight(date))
            .sorted(Comparator.comparing(Reservation::getRoomNumber))
            .toList();

        for (Reservation reservation : occupiedReservations) {
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(reservation.getRoomType().name()).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getCheckIn().format(DATE_FORMATTER)).append(",");
            csv.append(reservation.getCheckOut().format(DATE_FORMATTER)).append("\n");
        }

        int totalRooms = roomRepository.findAll().size();
        int occupiedRooms = occupiedReservations.size();
        csv.append("total,").append(occupiedRooms).append("/").append(totalRooms);

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        LocalDate startOfMonth = month.atDay(1);
        LocalDate endOfMonth = month.atEndOfMonth();

        List<Reservation> reservations = reservationRepository.findAll().stream()
            .filter(r -> !r.getCheckIn().isBefore(startOfMonth) && !r.getCheckIn().isAfter(endOfMonth))
            .filter(r -> r.getNetAmount().compareTo(BigDecimal.ZERO) > 0)
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                .thenComparing(Reservation::getId))
            .toList();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal netAmount = reservation.getNetAmount();
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(reservation.getCheckIn().format(DATE_FORMATTER)).append(",");
            csv.append(reservation.getCheckOut().format(DATE_FORMATTER)).append(",");
            csv.append(reservation.getStatus().name()).append(",");
            csv.append(formatAmount(netAmount)).append("\n");

            totalAmount = totalAmount.add(netAmount);
        }

        csv.append("total,").append(formatAmount(totalAmount));

        return csv.toString();
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}

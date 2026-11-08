package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class ReportService {
    private static final DateTimeFormatter CSV_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final ReservationService reservationService;
    private final RoomRepository roomRepository;

    public ReportService(ReservationService reservationService, RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> occupiedReservations = reservationService.getOccupancyForDate(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : occupiedReservations) {
            Room room = roomRepository.findByNumber(reservation.roomNumber()).orElseThrow();
            csv.append(String.format("%d,%s,%d,%s,%s\n",
                reservation.roomNumber(),
                room.type(),
                reservation.id(),
                formatCsvDate(reservation.checkIn()),
                formatCsvDate(reservation.checkOut())
            ));
        }

        csv.append(String.format("total,%d/%d\n", occupiedReservations.size(), allRooms.size()));

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationService.getRevenueForMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal revenue = reservation.amountPaid().subtract(
                reservation.refund() != null ? reservation.refund() : BigDecimal.ZERO
            );

            csv.append(String.format(Locale.US, "%d,%d,%s,%s,%s,%.2f\n",
                reservation.id(),
                reservation.roomNumber(),
                formatCsvDate(reservation.checkIn()),
                formatCsvDate(reservation.checkOut()),
                reservation.status(),
                revenue
            ));

            total = total.add(revenue);
        }

        csv.append(String.format(Locale.US, "total,%.2f\n", total));

        return csv.toString();
    }

    private String formatCsvDate(LocalDate date) {
        return date.format(CSV_DATE_FORMATTER);
    }
}

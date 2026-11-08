package com.pousadamarealta.application.service;

import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.domain.repository.ReservationRepository;
import com.pousadamarealta.domain.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private static final DateTimeFormatter CSV_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> occupiedReservations = reservationRepository.findPaidByDate(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : occupiedReservations) {
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                reservation.getRoomNumber(),
                room.getType().name(),
                reservation.getId(),
                reservation.getCheckIn().format(CSV_DATE_FORMATTER),
                reservation.getCheckOut().format(CSV_DATE_FORMATTER)));
        }

        csv.append(String.format("total,%d/%d", occupiedReservations.size(), allRooms.size()));

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(
            month.getYear(), month.getMonthValue());

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal amount = calculateRevenue(reservation);
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(String.format(Locale.US, "%d,%s,%s,%s,%s,%.2f\n",
                    reservation.getId(),
                    reservation.getRoomNumber(),
                    reservation.getCheckIn().format(CSV_DATE_FORMATTER),
                    reservation.getCheckOut().format(CSV_DATE_FORMATTER),
                    reservation.getStatus().name(),
                    amount));
                total = total.add(amount);
            }
        }

        csv.append(String.format(Locale.US, "total,%.2f", total));

        return csv.toString();
    }

    private BigDecimal calculateRevenue(Reservation reservation) {
        BigDecimal paid = reservation.getAmountPaid();
        BigDecimal refunded = reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refunded);
    }
}

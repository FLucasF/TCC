package com.pousadamarealta.service;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
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
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findPaidReservationsForNight(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(room.getType().name()).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getCheckIn().format(DATE_FORMATTER)).append(",");
            csv.append(reservation.getCheckOut().format(DATE_FORMATTER)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findReservationsStartingInMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal revenue = calculateRevenue(reservation);

            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(reservation.getId()).append(",");
                csv.append(reservation.getRoomNumber()).append(",");
                csv.append(reservation.getCheckIn().format(DATE_FORMATTER)).append(",");
                csv.append(reservation.getCheckOut().format(DATE_FORMATTER)).append(",");
                csv.append(reservation.getStatus().name()).append(",");
                csv.append(formatAmount(revenue)).append("\n");

                totalRevenue = totalRevenue.add(revenue);
            }
        }

        csv.append("total,").append(formatAmount(totalRevenue));

        return csv.toString();
    }

    private BigDecimal calculateRevenue(Reservation reservation) {
        BigDecimal revenue = reservation.getAmountPaid();

        if (reservation.getRefund() != null) {
            revenue = revenue.subtract(reservation.getRefund());
        }

        return revenue;
    }

    private String formatAmount(BigDecimal amount) {
        return String.format(Locale.US, "%.2f", amount);
    }
}

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
        int totalRooms = roomRepository.findAll().size();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                    reservation.getRoomNumber(),
                    room.getType(),
                    reservation.getId(),
                    reservation.getCheckIn().format(DATE_FORMATTER),
                    reservation.getCheckOut().format(DATE_FORMATTER)));
        }

        csv.append(String.format("total,%d/%d", reservations.size(), totalRooms));

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal amount = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                amount = amount.subtract(reservation.getRefund());
            }

            csv.append(String.format("%d,%s,%s,%s,%s,%s\n",
                    reservation.getId(),
                    reservation.getRoomNumber(),
                    reservation.getCheckIn().format(DATE_FORMATTER),
                    reservation.getCheckOut().format(DATE_FORMATTER),
                    reservation.getStatus(),
                    formatAmount(amount)));

            total = total.add(amount);
        }

        csv.append(String.format("total,%s", formatAmount(total)));

        return csv.toString();
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2).toString().replace(",", ".");
    }
}

package com.pousadamarealta.service;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findByDate(date);
        int totalRooms = roomRepository.findAll().size();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                reservation.getRoomNumber(),
                getRoomType(reservation.getRoomNumber()),
                reservation.getId(),
                reservation.getCheckIn().format(DATE_FORMAT),
                reservation.getCheckOut().format(DATE_FORMAT)
            ));
        }

        csv.append(String.format("total,%d/%d\n", reservations.size(), totalRooms));

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepository.findByMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal amount = calculateRevenue(reservation);
            total = total.add(amount);

            csv.append(String.format("%d,%s,%s,%s,%s,%s\n",
                reservation.getId(),
                reservation.getRoomNumber(),
                reservation.getCheckIn().format(DATE_FORMAT),
                reservation.getCheckOut().format(DATE_FORMAT),
                reservation.getStatus().name(),
                formatAmount(amount)
            ));
        }

        csv.append(String.format("total,%s\n", formatAmount(total)));

        return csv.toString();
    }

    private BigDecimal calculateRevenue(Reservation reservation) {
        BigDecimal revenue = reservation.getAmountPaid();
        if (reservation.getRefund() != null) {
            revenue = revenue.subtract(reservation.getRefund());
        }
        return revenue;
    }

    private String getRoomType(String roomNumber) {
        return roomRepository.findByNumber(roomNumber)
            .map(room -> room.getType().name())
            .orElse("UNKNOWN");
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}

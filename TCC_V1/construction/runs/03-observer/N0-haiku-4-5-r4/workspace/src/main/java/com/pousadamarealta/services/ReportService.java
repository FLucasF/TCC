package com.pousadamarealta.services;

import com.pousadamarealta.models.Reservation;
import com.pousadamarealta.models.ReservationStatus;
import com.pousadamarealta.repositories.ReservationRepository;
import com.pousadamarealta.repositories.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) throws Exception {
        List<Reservation> reservations = reservationRepository.findByDateAndConfirmed(date);

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation res : reservations) {
            csv.append(String.format("%d,%s,%d,%s,%s\n",
                res.getRoomNumber(),
                getRoomType(res.getRoomNumber()),
                res.getId(),
                res.getCheckIn(),
                res.getCheckOut()
            ));
        }

        int occupiedRooms = (int) reservations.stream()
            .map(Reservation::getRoomNumber)
            .distinct()
            .count();

        csv.append(String.format("total,%d/5\n", occupiedRooms));

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth yearMonth) throws Exception {
        List<Reservation> reservations = reservationRepository.findByMonthWithRevenue(
            yearMonth.getYear(),
            yearMonth.getMonthValue()
        );

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal amount = res.getAmountPaid();
            if (res.getRefund() != null) {
                amount = amount.subtract(res.getRefund());
            }

            csv.append(String.format("%d,%d,%s,%s,%s,%.2f\n",
                res.getId(),
                res.getRoomNumber(),
                res.getCheckIn(),
                res.getCheckOut(),
                res.getStatus(),
                amount
            ));

            total = total.add(amount);
        }

        csv.append(String.format("total,,,,%.2f\n", total));

        return csv.toString();
    }

    private String getRoomType(int roomNumber) {
        if (roomNumber < 200) return "STANDARD";
        if (roomNumber < 300) return "DELUXE";
        return "SUITE";
    }
}

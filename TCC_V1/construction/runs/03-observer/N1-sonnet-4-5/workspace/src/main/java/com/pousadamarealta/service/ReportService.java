package com.pousadamarealta.service;

import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findByDate(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                reservation.getRoomNumber(),
                room.getType(),
                reservation.getId(),
                reservation.getCheckIn(),
                reservation.getCheckOut()
            ));
        }

        csv.append(String.format("total,%d/%d\n", reservations.size(), allRooms.size()));

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal revenue = calculateRevenue(reservation);
            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(String.format("%d,%s,%s,%s,%s,%s\n",
                    reservation.getId(),
                    reservation.getRoomNumber(),
                    reservation.getCheckIn(),
                    reservation.getCheckOut(),
                    reservation.getStatus(),
                    formatAmount(revenue)
                ));
                total = total.add(revenue);
            }
        }

        csv.append(String.format("total,%s\n", formatAmount(total)));

        return csv.toString();
    }

    private BigDecimal calculateRevenue(Reservation reservation) {
        BigDecimal paid = reservation.getAmountPaid();
        BigDecimal refunded = reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refunded);
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}

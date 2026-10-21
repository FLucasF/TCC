package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReportService {
    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReportService(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> occupiedReservations = reservationService.getOccupancyForDate(date);
        List<Room> allRooms = roomService.getAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : occupiedReservations) {
            Room room = roomService.findByNumber(r.getRoomNumber()).orElseThrow();
            csv.append(String.format("%d,%s,%d,%s,%s\n",
                r.getRoomNumber(),
                room.getType(),
                r.getId(),
                r.getCheckIn(),
                r.getCheckOut()
            ));
        }

        csv.append(String.format("total,%d/%d", occupiedReservations.size(), allRooms.size()));

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> revenueReservations = reservationService.getRevenueForMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation r : revenueReservations) {
            BigDecimal revenue = r.getAmountPaid().subtract(
                r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO
            );
            total = total.add(revenue);

            csv.append(String.format("%d,%d,%s,%s,%s,%s\n",
                r.getId(),
                r.getRoomNumber(),
                r.getCheckIn(),
                r.getCheckOut(),
                r.getStatus(),
                formatCsvNumber(revenue)
            ));
        }

        csv.append(String.format("total,%s", formatCsvNumber(total)));

        return csv.toString();
    }

    private String formatCsvNumber(BigDecimal value) {
        return value.toString();
    }
}

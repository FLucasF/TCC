package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
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
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedRooms = 0;

        for (var room : roomService.getAllRooms()) {
            List<Reservation> reservations = reservationService.getReservationsForRoom(room.number, date);

            if (!reservations.isEmpty()) {
                for (Reservation res : reservations) {
                    csv.append(String.format("%d,%s,%d,%s,%s\n", room.number, room.type.name, res.getId(),
                            res.getCheckIn().toString(), res.getCheckOut().toString()));
                    occupiedRooms++;
                }
            }
        }

        csv.append(String.format("total,%d/%d\n", occupiedRooms, roomService.getAllRooms().size()));

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        double total = 0;

        List<Reservation> reservations = reservationService.getReservationsStartingInMonth(year, month);

        for (Reservation res : reservations) {
            double amount = res.getAmountPaid();
            if (res.getRefund() != null) {
                amount -= res.getRefund();
            }

            csv.append(String.format("%d,%d,%s,%s,%s,%.2f\n", res.getId(), res.getRoomNumber(),
                    res.getCheckIn().toString(), res.getCheckOut().toString(), res.getStatus().name(), amount));

            total += amount;
        }

        csv.append(String.format("total,%.2f\n", total));

        return csv.toString();
    }
}

package com.pousada.services;

import com.pousada.models.Reservation;
import com.pousada.models.ReservationStatus;
import com.pousada.models.Room;
import com.pousada.utils.DateUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReportService {
    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReportService(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<String> lines = new ArrayList<>();
        lines.add("room,type,reservation,check_in,check_out");

        int occupiedCount = 0;
        Map<Integer, Room> rooms = roomService.getAllRooms();

        for (int roomNumber : rooms.keySet()) {
            List<Reservation> reservations = reservationService.getReservationsForRoom(roomNumber, date);
            if (!reservations.isEmpty()) {
                Reservation res = reservations.get(0);
                if (res.getStatus() != ReservationStatus.CANCELLED) {
                    Room room = rooms.get(roomNumber);
                    lines.add(roomNumber + "," + room.getType() + "," + res.getId() + ","
                            + DateUtils.formatDateISO(res.getCheckIn()) + ","
                            + DateUtils.formatDateISO(res.getCheckOut()));
                    occupiedCount++;
                }
            }
        }

        lines.add("total," + occupiedCount + "/" + rooms.size());

        return String.join("\n", lines);
    }

    public String generateRevenueReport(int year, int month) {
        List<String> lines = new ArrayList<>();
        lines.add("reservation,room,check_in,check_out,status,amount");

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Reservation> reservations = new ArrayList<>(reservationService.getReservationsForMonth(year, month));

        reservations.sort((a, b) -> {
            int dateComp = a.getCheckIn().compareTo(b.getCheckIn());
            if (dateComp != 0) {
                return dateComp;
            }
            return Long.compare(a.getId(), b.getId());
        });

        for (Reservation res : reservations) {
            BigDecimal amount = res.getAmountPaid().subtract(res.getRefund() != null ? res.getRefund() : BigDecimal.ZERO);
            if (amount.signum() > 0) {
                lines.add(res.getId() + "," + res.getRoomNumber() + ","
                        + DateUtils.formatDateISO(res.getCheckIn()) + ","
                        + DateUtils.formatDateISO(res.getCheckOut()) + ","
                        + res.getStatus().toString() + ","
                        + formatAmount(amount));
                totalAmount = totalAmount.add(amount);
            }
        }

        lines.add("total," + formatAmount(totalAmount));

        return String.join("\n", lines);
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%.2f", amount);
    }
}

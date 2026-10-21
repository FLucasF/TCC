package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReportService {
    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReportService(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public String getOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupied = 0;
        List<Reservation> reservations = reservationService.getAllReservations();

        for (var room : roomService.getAllRooms()) {
            for (var res : reservations) {
                if (res.roomNumber == room.number() &&
                    (res.status == ReservationStatus.CONFIRMED ||
                     res.status == ReservationStatus.CHECKED_IN ||
                     res.status == ReservationStatus.CHECKED_OUT)) {

                    if (isRoomOccupiedOnDate(res, date)) {
                        csv.append(String.format("%d,%s,%d,%s,%s\n",
                            res.roomNumber,
                            room.type().name,
                            res.id,
                            formatDate(res.checkIn),
                            formatDate(res.checkOut)
                        ));
                        occupied++;
                        break;
                    }
                }
            }
        }

        csv.append(String.format("total,%d/%d\n", occupied, roomService.getAllRooms().size()));
        return csv.toString();
    }

    public String getRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        double total = 0;
        List<Reservation> reservations = reservationService.getAllReservations();

        List<Reservation> sorted = reservations.stream()
            .filter(r -> YearMonth.from(r.checkIn).equals(month))
            .filter(r -> r.status == ReservationStatus.CONFIRMED ||
                         r.status == ReservationStatus.CHECKED_IN ||
                         r.status == ReservationStatus.CHECKED_OUT ||
                         r.status == ReservationStatus.CANCELLED)
            .sorted((a, b) -> {
                int dateComp = a.checkIn.compareTo(b.checkIn);
                if (dateComp != 0) return dateComp;
                return Long.compare(a.id, b.id);
            })
            .toList();

        for (var res : sorted) {
            double amount = res.amountPaid;
            if (res.refund != null && res.refund > 0) {
                amount -= res.refund;
            }

            if (amount > 0 || res.status == ReservationStatus.CANCELLED) {
                csv.append(String.format("%d,%d,%s,%s,%s,%.2f\n",
                    res.id,
                    res.roomNumber,
                    formatDate(res.checkIn),
                    formatDate(res.checkOut),
                    res.status.name(),
                    amount
                ));
                total += amount;
            }
        }

        csv.append(String.format("total,,,,,%.2f\n", total));
        return csv.toString();
    }

    private boolean isRoomOccupiedOnDate(Reservation res, LocalDate date) {
        return !date.isBefore(res.checkIn) && date.isBefore(res.checkOut);
    }

    private String formatDate(LocalDate date) {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }
}

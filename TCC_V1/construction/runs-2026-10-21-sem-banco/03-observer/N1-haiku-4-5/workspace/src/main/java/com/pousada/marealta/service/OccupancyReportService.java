package com.pousada.marealta.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.ReservationStatus;
import com.pousada.marealta.model.Room;
import com.pousada.marealta.util.FormatHelper;

public class OccupancyReportService {
    private final ReservationService reservationService;
    private final RoomService roomService;

    public OccupancyReportService(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> occupiedRooms = new ArrayList<>();

        for (Room room : roomService.getAllRooms()) {
            for (Reservation res : reservationService.getAllReservations()) {
                if (res.roomNumber == room.number &&
                    res.status != ReservationStatus.CANCELLED &&
                    (res.status == ReservationStatus.CONFIRMED || res.status == ReservationStatus.CHECKED_IN ||
                     res.status == ReservationStatus.CHECKED_OUT)) {
                    if (!date.isBefore(res.checkIn) && date.isBefore(res.checkOut)) {
                        occupiedRooms.add(res);
                    }
                }
            }
        }

        occupiedRooms.sort((a, b) -> Integer.compare(a.roomNumber, b.roomNumber));

        for (Reservation res : occupiedRooms) {
            Room room = roomService.getRoomByNumber(res.roomNumber).get();
            csv.append(res.roomNumber).append(",")
                .append(room.type.name).append(",")
                .append(res.id).append(",")
                .append(FormatHelper.formatIsoDate(res.checkIn)).append(",")
                .append(FormatHelper.formatIsoDate(res.checkOut)).append("\n");
        }

        csv.append("total,").append(occupiedRooms.size()).append("/5\n");

        return csv.toString();
    }
}

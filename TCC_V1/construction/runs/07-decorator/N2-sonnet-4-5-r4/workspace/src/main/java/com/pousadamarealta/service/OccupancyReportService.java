package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OccupancyReportService {
    private static final int TOTAL_ROOMS = 5;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationService reservationService;
    private final RoomService roomService;

    public OccupancyReportService(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public String generateReport(LocalDate date) {
        List<Reservation> reservations = reservationService.getReservationsByNight(date);

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                reservation.getRoomNumber(),
                room.getType().name(),
                reservation.getId(),
                reservation.getCheckIn().format(DATE_FORMAT),
                reservation.getCheckOut().format(DATE_FORMAT)
            ));
        }

        csv.append(String.format("total,%d/%d", reservations.size(), TOTAL_ROOMS));

        return csv.toString();
    }
}

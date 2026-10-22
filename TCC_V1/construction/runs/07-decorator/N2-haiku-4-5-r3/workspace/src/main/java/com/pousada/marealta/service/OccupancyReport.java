package com.pousada.marealta.service;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.Room;
import com.pousada.marealta.infrastructure.ReservationRepository;
import com.pousada.marealta.infrastructure.RoomRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class OccupancyReport {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private static final int TOTAL_ROOMS = 5;

    public OccupancyReport(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generate(LocalDate date) throws Exception {
        List<Reservation> reservations = reservationRepository.findByDate(date);

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation res : reservations) {
            Optional<Room> room = roomRepository.findByNumber(res.getRoomNumber());
            String roomType = room.map(r -> r.getType().getLabel()).orElse("UNKNOWN");
            csv.append(res.getRoomNumber()).append(",")
               .append(roomType).append(",")
               .append(res.getId()).append(",")
               .append(res.getCheckIn()).append(",")
               .append(res.getCheckOut()).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(TOTAL_ROOMS).append("\n");

        return csv.toString();
    }
}

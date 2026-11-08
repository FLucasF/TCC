package com.marealta.report;

import com.marealta.db.ReservationRepository;
import com.marealta.db.RoomRepository;
import com.marealta.model.Reservation;
import com.marealta.model.Room;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OccupancyReport implements Report {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public OccupancyReport(RoomRepository roomRepository, ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    public String generate(LocalDate date) {
        List<Room> rooms = roomRepository.findAll();
        StringBuilder csv = new StringBuilder();

        csv.append("Pousada Maré Alta,occupancy,").append(date.format(DATE_FORMAT)).append("\n");
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedCount = 0;
        for (Room room : rooms) {
            List<Reservation> reservations = reservationRepository.findConfirmedOrPaidByRoomAndDate(room.number(), date);
            if (!reservations.isEmpty()) {
                for (Reservation res : reservations) {
                    csv.append(room.number()).append(",")
                        .append(room.type()).append(",")
                        .append(res.getId()).append(",")
                        .append(res.getCheckIn().format(DATE_FORMAT)).append(",")
                        .append(res.getCheckOut().format(DATE_FORMAT)).append("\n");
                    occupiedCount++;
                }
            }
        }

        csv.append("total,").append(occupiedCount).append("/").append(rooms.size());

        return csv.toString();
    }
}

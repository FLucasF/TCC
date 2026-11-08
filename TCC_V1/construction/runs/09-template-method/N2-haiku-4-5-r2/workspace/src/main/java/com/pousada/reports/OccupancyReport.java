package com.pousada.reports;

import com.pousada.infrastructure.ReservationRepository;
import com.pousada.infrastructure.RoomRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OccupancyReport extends Report {
    private final ReservationRepository reservationRepository = new ReservationRepository();
    private final RoomRepository roomRepository = new RoomRepository();

    @Override
    protected String getReportName() {
        return "occupancy";
    }

    @Override
    protected String getReportPeriod(LocalDate date) {
        return date.toString();
    }

    @Override
    protected String generateHeader() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    protected List<String> generateRows(LocalDate date) throws Exception {
        List<String> rows = new ArrayList<>();
        var rooms = roomRepository.findAll();

        for (var room : rooms) {
            var reservations = reservationRepository.findByRoomAndNight(room.getNumber(), date);
            if (!reservations.isEmpty()) {
                var reservation = reservations.get(0);
                rows.add(String.format("%d,%s,%d,%s,%s",
                    room.getNumber(),
                    room.getType().getDisplayName(),
                    reservation.getId(),
                    reservation.getCheckIn(),
                    reservation.getCheckOut()
                ));
            }
        }

        return rows;
    }

    @Override
    protected String generateTotal(LocalDate date) throws Exception {
        var rooms = roomRepository.findAll();
        int occupiedCount = 0;

        for (var room : rooms) {
            var reservations = reservationRepository.findByRoomAndNight(room.getNumber(), date);
            if (!reservations.isEmpty()) {
                occupiedCount++;
            }
        }

        return String.format("total,%d/%d", occupiedCount, rooms.size());
    }
}

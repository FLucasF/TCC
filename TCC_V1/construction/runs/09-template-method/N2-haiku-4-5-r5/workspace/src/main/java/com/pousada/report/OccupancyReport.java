package com.pousada.report;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OccupancyReport extends Report {
    private final LocalDate date;
    private final List<Room> allRooms;
    private final List<Reservation> occupiedReservations;

    public OccupancyReport(LocalDate date) {
        this.date = date;
        this.allRooms = RoomRepository.findAll();
        this.occupiedReservations = new ArrayList<>();

        for (Room room : allRooms) {
            List<Reservation> reservations = ReservationRepository.findByRoomAndDate(room.number, date);
            for (Reservation res : reservations) {
                if (res.status.name().equals("CONFIRMED") || res.status.name().equals("CHECKED_IN") || res.status.name().equals("CHECKED_OUT")) {
                    occupiedReservations.add(res);
                }
            }
        }
    }

    @Override
    protected String buildTitle() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return "Pousada Maré Alta,occupancy," + date.format(formatter);
    }

    @Override
    protected String buildHeader() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    protected List<String> buildDataLines() {
        List<String> lines = new ArrayList<>();

        for (Room room : allRooms) {
            Reservation occupied = occupiedReservations.stream()
                    .filter(r -> r.roomNumber == room.number)
                    .findFirst()
                    .orElse(null);

            if (occupied != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                lines.add(room.number + "," + room.type + "," + occupied.id + "," +
                        occupied.checkIn.format(formatter) + "," +
                        occupied.checkOut.format(formatter));
            }
        }

        return lines;
    }

    @Override
    protected String buildTotalLine() {
        return "total," + occupiedReservations.size() + "/" + allRooms.size();
    }
}

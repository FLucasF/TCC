package com.pousada.report.generators;

import com.pousada.domain.model.Reservation;
import com.pousada.domain.model.Room;
import com.pousada.infrastructure.repository.RoomRepository;
import com.pousada.report.ReportGenerator;

import java.time.LocalDate;
import java.util.List;

public class OccupancyReportGenerator implements ReportGenerator {

    @Override
    public String getName() {
        return "occupancy";
    }

    @Override
    public String generate(String period, List<Reservation> reservations, RoomRepository roomRepository) {
        LocalDate date = LocalDate.parse(period);

        List<Reservation> occupiedReservations = reservations.stream()
            .filter(Reservation::isPaid)
            .filter(r -> r.occupiesNight(date))
            .sorted((r1, r2) -> r1.getRoomNumber().compareTo(r2.getRoomNumber()))
            .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,").append(getName()).append(",").append(period).append("\n");
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : occupiedReservations) {
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            csv.append(reservation.getRoomNumber()).append(",")
               .append(room.getType()).append(",")
               .append(reservation.getId()).append(",")
               .append(reservation.getCheckIn()).append(",")
               .append(reservation.getCheckOut()).append("\n");
        }

        int totalRooms = roomRepository.getTotalRooms();
        csv.append("total,").append(occupiedReservations.size()).append("/").append(totalRooms);

        return csv.toString();
    }
}

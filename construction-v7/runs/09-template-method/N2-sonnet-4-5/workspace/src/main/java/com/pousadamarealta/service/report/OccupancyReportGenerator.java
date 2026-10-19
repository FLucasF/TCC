package com.pousadamarealta.service.report;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class OccupancyReportGenerator implements ReportGenerator {
    @Override
    public String generate(List<Reservation> reservations, List<Room> rooms, String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);

        List<Reservation> occupying = reservations.stream()
            .filter(Reservation::isPaid)
            .filter(r -> r.occupiesNight(date))
            .sorted((r1, r2) -> r1.getRoomNumber().compareTo(r2.getRoomNumber()))
            .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,occupancy,").append(dateStr).append("\n");
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : occupying) {
            Room room = rooms.stream()
                .filter(r -> r.getNumber().equals(reservation.getRoomNumber()))
                .findFirst()
                .orElseThrow();

            csv.append(reservation.getRoomNumber()).append(",")
               .append(room.getType()).append(",")
               .append(reservation.getId()).append(",")
               .append(reservation.getCheckIn()).append(",")
               .append(reservation.getCheckOut()).append("\n");
        }

        csv.append("total,").append(occupying.size()).append("/").append(rooms.size());

        return csv.toString();
    }
}

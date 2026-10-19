package com.pousada.strategy;

import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.domain.Room;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class OccupancyReportStrategy implements ReportStrategy {
    @Override
    public String generate(List<Reservation> reservations, List<Room> rooms, Object parameter) {
        LocalDate date = (LocalDate) parameter;
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,occupancy,").append(date.format(dateFormatter)).append("\n");
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> occupiedReservations = reservations.stream()
                .filter(r -> isPaid(r.getStatus()))
                .filter(r -> occupiesNight(r, date))
                .sorted(Comparator.comparingInt(Reservation::getRoomNumber))
                .toList();

        for (Reservation reservation : occupiedReservations) {
            Room room = rooms.stream()
                    .filter(r -> r.getNumber() == reservation.getRoomNumber())
                    .findFirst()
                    .orElseThrow();

            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(room.getType()).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getCheckIn().format(dateFormatter)).append(",");
            csv.append(reservation.getCheckOut().format(dateFormatter)).append("\n");
        }

        csv.append("total,").append(occupiedReservations.size()).append("/").append(rooms.size());

        return csv.toString();
    }

    @Override
    public String getReportName() {
        return "occupancy";
    }

    private boolean isPaid(ReservationStatus status) {
        return status == ReservationStatus.CONFIRMED ||
               status == ReservationStatus.CHECKED_IN ||
               status == ReservationStatus.CHECKED_OUT;
    }

    private boolean occupiesNight(Reservation reservation, LocalDate night) {
        return !reservation.getCheckIn().isAfter(night) &&
               reservation.getCheckOut().isAfter(night);
    }
}

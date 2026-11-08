package com.pousadamareaalta.service.report;

import com.pousadamareaalta.database.*;
import com.pousadamareaalta.domain.*;
import com.pousadamareaalta.util.FormatterUtil;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public class OccupancyReport implements ReportGenerator {
    private final LocalDate date;

    public OccupancyReport(LocalDate date) {
        this.date = date;
    }

    @Override
    public String generate() throws SQLException {
        List<Reservation> reservations = ReservationRepository.findByDateOccupancy(date);
        List<Room> allRooms = RoomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,occupancy,").append(FormatterUtil.formatDateISO(date)).append("\n");
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Room room : allRooms) {
            Optional<Reservation> reservation = reservations.stream()
                .filter(r -> r.roomNumber() == room.number())
                .findFirst();

            if (reservation.isPresent()) {
                Reservation res = reservation.get();
                csv.append(res.roomNumber()).append(",");
                csv.append(room.type()).append(",");
                csv.append(res.id()).append(",");
                csv.append(FormatterUtil.formatDateISO(res.checkIn())).append(",");
                csv.append(FormatterUtil.formatDateISO(res.checkOut())).append("\n");
            }
        }

        long occupiedCount = reservations.size();
        long totalCount = allRooms.size();
        csv.append("total,").append(occupiedCount).append("/").append(totalCount);

        return csv.toString();
    }

    @Override
    public String getReportName() {
        return "occupancy";
    }
}

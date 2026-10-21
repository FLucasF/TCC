package com.pousadamarealta.service;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.util.DateUtil;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OccupancyReportStrategy implements ReportStrategy {
    @Override
    public String generate(ReservationRepository resRepo, RoomRepository roomRepo, Object param) {
        LocalDate date = (LocalDate) param;
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        int totalOccupied = 0;
        List<Reservation> occupied = new ArrayList<>();

        // Collect occupied rooms
        for (Room room : roomRepo.findAll()) {
            for (Reservation res : resRepo.findAll()) {
                if (res.roomNumber == room.number &&
                    res.status == ReservationStatus.CONFIRMED &&
                    !res.checkIn.isAfter(date) &&
                    res.checkOut.isAfter(date)) {
                    occupied.add(res);
                    break;
                }
            }
        }

        // Sort by room number
        occupied.sort((a, b) -> Integer.compare(a.roomNumber, b.roomNumber));

        for (Reservation res : occupied) {
            Room room = roomRepo.findByNumber(res.roomNumber).orElseThrow();
            csv.append(res.roomNumber).append(",")
               .append(room.getTypeString()).append(",")
               .append(res.id).append(",")
               .append(DateUtil.formatDateISO(res.checkIn)).append(",")
               .append(DateUtil.formatDateISO(res.checkOut)).append("\n");
            totalOccupied++;
        }

        int totalRooms = 0;
        for (Room room : roomRepo.findAll()) {
            totalRooms++;
        }
        csv.append("total,").append(totalOccupied).append("/").append(totalRooms).append("\n");

        return csv.toString();
    }
}

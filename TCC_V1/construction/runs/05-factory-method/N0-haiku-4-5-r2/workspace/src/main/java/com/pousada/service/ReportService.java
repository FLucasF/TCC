package com.pousada.service;

import com.pousada.db.Database;
import com.pousada.model.Reservation;
import com.pousada.model.Room;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportService {
    private final ReservationRepository reservationRepo;
    private final RoomRepository roomRepo;

    public ReportService(Database db) {
        this.reservationRepo = new ReservationRepository(db);
        this.roomRepo = new RoomRepository(db);
    }

    public String getOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepo.findByDateForOccupancy(date);
        List<Room> allRooms = roomRepo.findAll();

        Map<Integer, Reservation> occupancy = new HashMap<>();
        for (Reservation r : reservations) {
            occupancy.put(r.roomNumber, r);
        }

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Room room : allRooms) {
            Reservation res = occupancy.get(room.number);
            if (res != null) {
                csv.append(room.number).append(",")
                   .append(room.type).append(",")
                   .append(res.id).append(",")
                   .append(res.checkIn).append(",")
                   .append(res.checkOut).append("\n");
            }
        }

        int occupied = occupancy.size();
        int total = allRooms.size();
        csv.append("total,").append(occupied).append("/").append(total);

        return csv.toString();
    }

    public String getRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepo.findByMonthForRevenue(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation r : reservations) {
            if (!"CANCELLED".equals(r.status) || r.amountPaid.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal netAmount = r.amountPaid;
                if (r.refund != null) {
                    netAmount = r.amountPaid.subtract(r.refund);
                }

                csv.append(r.id).append(",")
                   .append(r.roomNumber).append(",")
                   .append(r.checkIn).append(",")
                   .append(r.checkOut).append(",")
                   .append(r.status).append(",")
                   .append(String.format("%.2f", netAmount)).append("\n");

                total = total.add(netAmount);
            }
        }

        csv.append("total,,,,").append(String.format("%.2f", total));

        return csv.toString();
    }
}

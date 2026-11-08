package com.pousadamarealta.service;

import com.pousadamarealta.db.ReservationRepository;
import com.pousadamarealta.db.RoomRepository;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.util.DateFormatter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ReportService {
    public static String generateOccupancyReport(LocalDate date) throws SQLException {
        List<Room> rooms = RoomRepository.findAll();
        List<Reservation> reservations = ReservationRepository.findByDateOccupancy(date);

        Map<Integer, Reservation> resMap = reservations.stream()
            .collect(Collectors.toMap(r -> r.roomNumber, r -> r));

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedCount = 0;
        for (Room room : rooms) {
            if (resMap.containsKey(room.number())) {
                Reservation res = resMap.get(room.number());
                csv.append(String.format("%d,%s,%d,%s,%s\n",
                    room.number(),
                    room.type(),
                    res.id,
                    DateFormatter.formatIsoDate(res.checkIn),
                    DateFormatter.formatIsoDate(res.checkOut)
                ));
                occupiedCount++;
            }
        }

        csv.append(String.format("total,%d/%d", occupiedCount, rooms.size()));
        return csv.toString();
    }

    public static String generateRevenueReport(LocalDate monthStart) throws SQLException {
        List<Reservation> reservations = ReservationRepository.findByMonthStartDate(monthStart);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation res : reservations) {
            BigDecimal amount = res.amountPaid;
            if (res.refund != null) {
                amount = amount.subtract(res.refund);
            }

            if (amount.signum() > 0) {
                csv.append(String.format(java.util.Locale.US, "%d,%d,%s,%s,%s,%.2f\n",
                    res.id,
                    res.roomNumber,
                    DateFormatter.formatIsoDate(res.checkIn),
                    DateFormatter.formatIsoDate(res.checkOut),
                    res.status,
                    amount
                ));

                total = total.add(amount);
            }
        }

        csv.append(String.format(java.util.Locale.US, "total,%.2f", total));
        return csv.toString();
    }
}

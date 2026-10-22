package com.pousadamarealta.service;

import com.pousadamarealta.db.ReservationRepository;
import com.pousadamarealta.db.RoomRepository;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) throws Exception {
        List<Reservation> reservations = reservationRepository.findByDateForOccupancy(date);
        Map<Integer, Room> roomMap = buildRoomMap();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation res : reservations) {
            Room room = roomMap.get(res.getRoomNumber());
            if (room != null) {
                csv.append(res.getRoomNumber()).append(",");
                csv.append(room.type()).append(",");
                csv.append(res.getId()).append(",");
                csv.append(formatDate(res.getCheckIn())).append(",");
                csv.append(formatDate(res.getCheckOut())).append("\n");
            }
        }

        int occupiedRooms = reservations.size();
        int totalRooms = roomMap.size();
        csv.append("total,").append(occupiedRooms).append("/").append(totalRooms);

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) throws Exception {
        List<Reservation> reservations = reservationRepository.findByMonth(year, month);
        Map<Integer, Room> roomMap = buildRoomMap();

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal amount = res.getAmountPaid();
            if (res.getRefund() != null) {
                amount = amount.subtract(res.getRefund());
            }

            csv.append(res.getId()).append(",");
            csv.append(res.getRoomNumber()).append(",");
            csv.append(formatDate(res.getCheckIn())).append(",");
            csv.append(formatDate(res.getCheckOut())).append(",");
            csv.append(res.getStatus()).append(",");
            csv.append(formatMoney(amount)).append("\n");

            totalAmount = totalAmount.add(amount);
        }

        csv.append("total,").append(formatMoney(totalAmount));

        return csv.toString();
    }

    private Map<Integer, Room> buildRoomMap() throws Exception {
        Map<Integer, Room> map = new HashMap<>();
        for (Room room : roomRepository.findAll()) {
            map.put(room.number(), room);
        }
        return map;
    }

    private String formatDate(LocalDate date) {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private String formatMoney(BigDecimal value) {
        return value.toPlainString();
    }
}

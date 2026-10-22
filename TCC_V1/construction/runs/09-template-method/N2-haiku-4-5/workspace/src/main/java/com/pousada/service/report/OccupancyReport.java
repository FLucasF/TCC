package com.pousada.service.report;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.repo.ReservationRepository;
import com.pousada.repo.RoomRepository;
import com.pousada.util.DateUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class OccupancyReport extends Report {
    private final ReservationRepository reservationRepo;
    private final RoomRepository roomRepo;
    private final LocalDate date;
    private final int totalRooms = 5;

    public OccupancyReport(ReservationRepository reservationRepo, RoomRepository roomRepo, LocalDate date) throws Exception {
        super("occupancy", DateUtil.formatDateForCsv(date));
        this.reservationRepo = reservationRepo;
        this.roomRepo = roomRepo;
        this.date = date;
    }

    @Override
    protected String getColumnHeader() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    protected List<String> getDataRows() {
        List<String> rows = new ArrayList<>();
        try {
            List<Reservation> reservations = reservationRepo.findByDateAndPaid(date);

            Map<Integer, Reservation> roomReservationMap = new LinkedHashMap<>();
            for (Reservation r : reservations) {
                roomReservationMap.put(r.getRoomNumber(), r);
            }

            List<Room> allRooms = roomRepo.findAll();
            for (Room room : allRooms) {
                if (roomReservationMap.containsKey(room.getNumber())) {
                    Reservation res = roomReservationMap.get(room.getNumber());
                    rows.add(formatRow(room, res));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    @Override
    protected String getTotalLine() {
        try {
            List<Reservation> reservations = reservationRepo.findByDateAndPaid(date);
            return "total," + reservations.size() + "/" + totalRooms;
        } catch (Exception e) {
            return "total,0/" + totalRooms;
        }
    }

    private String formatRow(Room room, Reservation reservation) {
        return room.getNumber() + "," +
               room.getType().getDisplayName() + "," +
               reservation.getId() + "," +
               DateUtil.formatDateForCsv(reservation.getCheckIn()) + "," +
               DateUtil.formatDateForCsv(reservation.getCheckOut());
    }
}

package com.pousadamarealta.reports;

import com.pousadamarealta.database.ReservationDao;
import com.pousadamarealta.database.RoomDao;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.time.LocalDate;
import java.util.List;

public class OccupancyReport {
    private final ReservationDao reservationDao;
    private final RoomDao roomDao;

    public OccupancyReport(ReservationDao reservationDao, RoomDao roomDao) {
        this.reservationDao = reservationDao;
        this.roomDao = roomDao;
    }

    public String generate(LocalDate date) {
        List<Reservation> reservations = reservationDao.findPaidReservationsForNight(date);
        List<Room> allRooms = roomDao.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            Room room = roomDao.findByNumber(reservation.getRoomNumber()).orElseThrow();
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                reservation.getRoomNumber(),
                room.getType().name(),
                reservation.getId(),
                reservation.getCheckIn(),
                reservation.getCheckOut()
            ));
        }

        csv.append(String.format("total,%d/%d\n", reservations.size(), allRooms.size()));

        return csv.toString();
    }
}

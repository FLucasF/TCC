package com.pousada.reports;

import com.pousada.db.ReservationRepository;
import com.pousada.db.RoomRepository;
import com.pousada.models.Reservation;
import com.pousada.models.ReservationStatus;
import com.pousada.models.Room;
import com.pousada.utils.PricingUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public class OccupancyReport extends Report {
    private final RoomRepository roomRepo;
    private final ReservationRepository reservationRepo;
    private final LocalDate date;

    public OccupancyReport(RoomRepository roomRepo, ReservationRepository reservationRepo, LocalDate date) {
        this.roomRepo = roomRepo;
        this.reservationRepo = reservationRepo;
        this.date = date;
    }

    @Override
    public String generate() {
        try {
            StringBuilder sb = new StringBuilder();
            String title = generateTitle("occupancy", PricingUtils.formatDateIso(date));
            sb.append(title).append("\n");

            sb.append(generateCsvRow("room", "type", "reservation", "check_in", "check_out")).append("\n");

            List<Room> rooms = roomRepo.findAll();
            int occupiedCount = 0;

            for (Room room : rooms) {
                LocalDate checkInDay = date;
                LocalDate checkOutDay = date.plusDays(1);

                List<Reservation> reservations = reservationRepo.findByRoomAndDateRange(
                        room.getNumber(), checkInDay, checkOutDay);

                boolean occupied = false;
                for (Reservation res : reservations) {
                    if (res.getStatus() == ReservationStatus.CONFIRMED
                            || res.getStatus() == ReservationStatus.CHECKED_IN
                            || res.getStatus() == ReservationStatus.CHECKED_OUT) {
                        occupied = true;
                        sb.append(generateCsvRow(
                                String.valueOf(room.getNumber()),
                                room.getType().getLabel(),
                                String.valueOf(res.getId()),
                                PricingUtils.formatDateIso(res.getCheckIn()),
                                PricingUtils.formatDateIso(res.getCheckOut())
                        )).append("\n");
                        break;
                    }
                }

                if (occupied) {
                    occupiedCount++;
                }
            }

            sb.append(generateCsvRow("total", String.valueOf(occupiedCount) + "/" + rooms.size())).append("\n");

            return sb.toString();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

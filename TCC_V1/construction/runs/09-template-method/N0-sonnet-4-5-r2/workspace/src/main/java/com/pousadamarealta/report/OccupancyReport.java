package com.pousadamarealta.report;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.util.DateUtils;

import java.time.LocalDate;
import java.util.List;

public class OccupancyReport extends BaseReport {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public OccupancyReport() {
        this.reservationRepository = new ReservationRepository();
        this.roomRepository = new RoomRepository();
    }

    public String generate(LocalDate date) {
        addTitleLine("occupancy", DateUtils.formatISO(date));
        addHeaderLine("room", "type", "reservation", "check_in", "check_out");

        List<Reservation> reservations = reservationRepository.findByDate(date);

        for (Reservation r : reservations) {
            addDataLine(
                r.getRoomNumber(),
                roomRepository.findByNumber(r.getRoomNumber()).get().getType().name(),
                r.getId().toString(),
                DateUtils.formatISO(r.getCheckIn()),
                DateUtils.formatISO(r.getCheckOut())
            );
        }

        int totalRooms = roomRepository.findAll().size();
        int occupiedRooms = reservations.size();
        addDataLine("total", String.format("%d/%d", occupiedRooms, totalRooms));

        return super.generate();
    }
}

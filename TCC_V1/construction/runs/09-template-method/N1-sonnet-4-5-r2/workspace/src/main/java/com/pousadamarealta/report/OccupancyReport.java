package com.pousadamarealta.report;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OccupancyReport extends CsvReport {
    private final LocalDate date;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public OccupancyReport(LocalDate date, ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.date = date;
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    protected String getReportName() {
        return "occupancy";
    }

    @Override
    protected String getPeriod() {
        return date.format(dateFormatter);
    }

    @Override
    protected List<String> getColumnNames() {
        return List.of("room", "type", "reservation", "check_in", "check_out");
    }

    @Override
    protected List<List<String>> getDataRows() {
        List<Reservation> reservations = reservationRepository.findByDate(date);
        List<List<String>> rows = new ArrayList<>();

        for (Reservation reservation : reservations) {
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            rows.add(List.of(
                String.valueOf(reservation.getRoomNumber()),
                room.getType().name(),
                String.valueOf(reservation.getId()),
                reservation.getCheckIn().format(dateFormatter),
                reservation.getCheckOut().format(dateFormatter)
            ));
        }

        return rows;
    }

    @Override
    protected List<String> getTotalRow() {
        int occupied = reservationRepository.findByDate(date).size();
        int total = roomRepository.findAll().size();
        return List.of("total", occupied + "/" + total);
    }
}

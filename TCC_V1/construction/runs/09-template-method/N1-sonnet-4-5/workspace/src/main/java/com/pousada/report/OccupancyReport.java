package com.pousada.report;

import com.pousada.domain.Reservation;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OccupancyReport extends Report {
    private final LocalDate date;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public OccupancyReport(LocalDate date, ReservationRepository reservationRepository,
                          RoomRepository roomRepository) {
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
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Override
    protected String buildHeaderLine() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    protected List<String> buildDataLines() {
        List<String> lines = new ArrayList<>();
        List<Reservation> reservations = reservationRepository.findPaidByNight(date);

        reservations.stream()
                .sorted(Comparator.comparing(Reservation::getRoomNumber))
                .forEach(r -> {
                    lines.add(String.format("%s,%s,%d,%s,%s",
                            r.getRoomNumber(),
                            getRoomType(r.getRoomNumber()),
                            r.getId(),
                            r.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE),
                            r.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    ));
                });

        return lines;
    }

    @Override
    protected String buildTotalLine() {
        int occupiedRooms = reservationRepository.findPaidByNight(date).size();
        int totalRooms = roomRepository.getTotalRooms();
        return String.format("total,%d/%d", occupiedRooms, totalRooms);
    }

    private String getRoomType(String roomNumber) {
        return roomRepository.findByNumber(roomNumber)
                .map(room -> room.getType().name())
                .orElse("UNKNOWN");
    }
}

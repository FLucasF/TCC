package com.pousada.report;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OccupancyReport extends Report {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final LocalDate date;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private List<Reservation> reservations;
    private int totalRooms;

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
        return DATE_FORMATTER.format(date);
    }

    @Override
    protected String buildHeaderLine() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    protected List<String> buildDataLines() {
        reservations = reservationRepository.findPaidReservationsForNight(date);
        totalRooms = roomRepository.findAll().size();

        return reservations.stream()
            .map(this::formatReservationLine)
            .toList();
    }

    @Override
    protected String buildTotalLine() {
        return String.format("total,%d/%d", reservations.size(), totalRooms);
    }

    private String formatReservationLine(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new RuntimeException("Room not found: " + reservation.getRoomNumber()));

        return String.join(",",
            room.getNumber(),
            room.getType().name(),
            reservation.getId().toString(),
            DATE_FORMATTER.format(reservation.getCheckIn()),
            DATE_FORMATTER.format(reservation.getCheckOut())
        );
    }
}

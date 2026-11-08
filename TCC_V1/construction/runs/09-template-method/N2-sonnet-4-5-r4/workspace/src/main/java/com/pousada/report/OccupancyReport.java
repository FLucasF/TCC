package com.pousada.report;

import com.pousada.domain.Reservation;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OccupancyReport extends ReportGenerator {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public OccupancyReport(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    protected String getReportName() {
        return "occupancy";
    }

    @Override
    protected String generateHeader() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    protected List<String> generateDataLines(String period) {
        LocalDate date = LocalDate.parse(period);
        List<Reservation> reservations = reservationRepository.findPaidReservationsForDate(date);

        List<String> lines = new ArrayList<>();
        for (Reservation reservation : reservations) {
            var room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            lines.add(String.format("%d,%s,%d,%s,%s",
                reservation.getRoomNumber(),
                room.getType(),
                reservation.getId(),
                formatDate(reservation.getCheckIn()),
                formatDate(reservation.getCheckOut())));
        }
        return lines;
    }

    @Override
    protected String generateTotal(String period) {
        LocalDate date = LocalDate.parse(period);
        List<Reservation> reservations = reservationRepository.findPaidReservationsForDate(date);
        int totalRooms = roomRepository.findAll().size();
        return String.format("total,%d/%d", reservations.size(), totalRooms);
    }
}

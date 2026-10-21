package com.pousadamarealta.report;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OccupancyReport extends CsvReport {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final LocalDate date;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private List<Reservation> reservations;

    public OccupancyReport(LocalDate date, ReservationRepository reservationRepository,
                          RoomRepository roomRepository) {
        this.date = date;
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    protected String reportName() {
        return "occupancy";
    }

    @Override
    protected String period() {
        return date.format(DATE_FORMATTER);
    }

    @Override
    protected String headerLine() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    protected List<String> dataLines() {
        reservations = reservationRepository.findPaidByNight(date);
        List<String> lines = new ArrayList<>();

        for (Reservation r : reservations) {
            lines.add(String.format("%s,%s,%d,%s,%s",
                r.getRoomNumber(),
                roomRepository.findByNumber(r.getRoomNumber()).get().getType().name(),
                r.getId(),
                r.getCheckIn().format(DATE_FORMATTER),
                r.getCheckOut().format(DATE_FORMATTER)
            ));
        }

        return lines;
    }

    @Override
    protected String totalLine() {
        int occupied = reservations.size();
        int total = roomRepository.findAll().size();
        return String.format("total,%d/%d", occupied, total);
    }
}

package com.pousadamarealta.report;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OccupancyReport extends CsvReport {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final LocalDate date;
    private final List<Reservation> reservations;
    private final Map<String, Room> roomsByNumber;
    private final int totalRooms;

    public OccupancyReport(LocalDate date, List<Reservation> reservations,
                          Map<String, Room> roomsByNumber, int totalRooms) {
        this.date = date;
        this.reservations = reservations;
        this.roomsByNumber = roomsByNumber;
        this.totalRooms = totalRooms;
    }

    @Override
    protected String getReportName() {
        return "occupancy";
    }

    @Override
    protected String getPeriod() {
        return date.format(DATE_FORMATTER);
    }

    @Override
    protected String buildHeaderLine() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    protected List<String> buildDataLines() {
        return reservations.stream()
            .map(r -> {
                Room room = roomsByNumber.get(r.getRoomNumber());
                return String.format("%s,%s,%d,%s,%s",
                    r.getRoomNumber(),
                    room.type().name(),
                    r.getId(),
                    r.getCheckIn().format(DATE_FORMATTER),
                    r.getCheckOut().format(DATE_FORMATTER));
            })
            .collect(Collectors.toList());
    }

    @Override
    protected String buildTotalLine() {
        return String.format("total,%d/%d", reservations.size(), totalRooms);
    }
}

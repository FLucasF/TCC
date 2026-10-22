package com.pousadamarealta.service.report;

import com.pousadamarealta.domain.Reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class OccupancyReport extends Report {
    private final LocalDate date;
    private final List<Reservation> reservations;
    private final int totalRooms;

    public OccupancyReport(LocalDate date, List<Reservation> reservations, int totalRooms) {
        this.date = date;
        this.reservations = reservations;
        this.totalRooms = totalRooms;
    }

    @Override
    protected String getReportName() {
        return "occupancy";
    }

    @Override
    protected String getPeriod() {
        return formatCsvDate(date);
    }

    @Override
    protected String buildHeader() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    protected List<String> buildDataLines() {
        return reservations.stream()
                .map(this::buildReservationLine)
                .collect(Collectors.toList());
    }

    private String buildReservationLine(Reservation reservation) {
        return joinCsvFields(
                reservation.getRoomNumber(),
                reservation.getRoomType().name(),
                reservation.getId(),
                formatCsvDate(reservation.getCheckIn()),
                formatCsvDate(reservation.getCheckOut())
        );
    }

    @Override
    protected String buildTotal() {
        return String.format("total,%d/%d", reservations.size(), totalRooms);
    }
}

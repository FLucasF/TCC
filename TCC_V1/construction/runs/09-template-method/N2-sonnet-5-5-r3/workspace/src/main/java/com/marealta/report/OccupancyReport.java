package com.marealta.report;

import com.marealta.Reservation;

import java.time.LocalDate;
import java.util.List;

public final class OccupancyReport extends Report {
    private final ReportData data;
    private final LocalDate night;
    private final List<Reservation> occupying;

    public OccupancyReport(ReportData data, LocalDate night) {
        this.data = data;
        this.night = night;
        this.occupying = data.paidOccupying(night);
    }

    @Override
    protected String name() { return "occupancy"; }

    @Override
    protected String period() { return night.toString(); }

    @Override
    protected List<String> header() { return List.of("room", "type", "reservation", "check_in", "check_out"); }

    @Override
    protected List<List<String>> rows() {
        return occupying.stream().map(r -> List.of(
                String.valueOf(r.roomNumber()),
                data.rooms().find(r.roomNumber()).orElseThrow().type().name(),
                String.valueOf(r.id()),
                r.checkIn().toString(),
                r.checkOut().toString())).toList();
    }

    @Override
    protected List<String> total() {
        return List.of("total", occupying.size() + "/" + data.rooms().size());
    }
}

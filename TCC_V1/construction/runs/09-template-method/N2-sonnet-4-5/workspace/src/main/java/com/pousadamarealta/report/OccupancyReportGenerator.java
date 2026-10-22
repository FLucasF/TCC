package com.pousadamarealta.report;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.ReservationRepository;

import java.time.LocalDate;
import java.util.List;

public class OccupancyReportGenerator extends ReportGenerator {
    private final LocalDate date;
    private final ReservationRepository reservationRepository;
    private final int totalRooms;

    public OccupancyReportGenerator(LocalDate date, ReservationRepository reservationRepository, int totalRooms) {
        this.date = date;
        this.reservationRepository = reservationRepository;
        this.totalRooms = totalRooms;
    }

    @Override
    protected String getReportName() {
        return "occupancy";
    }

    @Override
    protected String getPeriod() {
        return formatDate(date);
    }

    @Override
    protected String buildHeaderLine() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    protected String buildDataLines() {
        List<Reservation> reservations = reservationRepository.findPaidByDate(date);
        StringBuilder lines = new StringBuilder();
        for (Reservation r : reservations) {
            lines.append(r.getRoomNumber()).append(",");
            lines.append(getRoomType(r.getRoomNumber())).append(",");
            lines.append(r.getId()).append(",");
            lines.append(formatDate(r.getCheckIn())).append(",");
            lines.append(formatDate(r.getCheckOut())).append("\n");
        }
        return lines.toString();
    }

    @Override
    protected String buildTotalLine() {
        List<Reservation> reservations = reservationRepository.findPaidByDate(date);
        return "total," + reservations.size() + "/" + totalRooms;
    }

    private String getRoomType(String roomNumber) {
        if (roomNumber.equals("101") || roomNumber.equals("102")) return "STANDARD";
        if (roomNumber.equals("201") || roomNumber.equals("202")) return "DELUXE";
        if (roomNumber.equals("301")) return "SUITE";
        return "UNKNOWN";
    }
}

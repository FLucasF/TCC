package br.com.pousadamarealta.report;

import br.com.pousadamarealta.domain.reservation.Reservation;
import br.com.pousadamarealta.domain.reservation.ReservationRepository;
import br.com.pousadamarealta.util.DateUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OccupancyReport extends ReportGenerator {
    private final ReservationRepository reservationRepository;
    private final LocalDate date;
    private List<Reservation> reservations;

    public OccupancyReport(ReservationRepository reservationRepository, LocalDate date) {
        this.reservationRepository = reservationRepository;
        this.date = date;
    }

    @Override
    protected String getReportName() {
        return "occupancy";
    }

    @Override
    protected List<String> getHeaders() {
        return Arrays.asList("room", "type", "reservation", "check_in", "check_out");
    }

    @Override
    protected List<String> getDataLines() {
        try {
            reservations = reservationRepository.findByDate(date);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return reservations.stream()
                .map(r -> String.format("%s,%s,%d,%s,%s",
                        r.getRoomNumber(),
                        getRoomTypeName(r.getRoomNumber()),
                        r.getId(),
                        DateUtils.formatIso(r.getCheckIn()),
                        DateUtils.formatIso(r.getCheckOut())))
                .collect(Collectors.toList());
    }

    @Override
    protected String getTotalLine() {
        int occupied = reservations.size();
        int total = 5; // Total de quartos da pousada
        return String.format("total,%d/%d", occupied, total);
    }

    private String getRoomTypeName(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "";
        };
    }
}

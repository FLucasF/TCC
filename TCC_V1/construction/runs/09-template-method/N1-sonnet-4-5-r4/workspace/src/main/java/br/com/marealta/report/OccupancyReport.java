package br.com.marealta.report;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.repository.ReservationRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OccupancyReport extends ReportGenerator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int TOTAL_ROOMS = 5;

    private final ReservationRepository reservationRepository;

    public OccupancyReport(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    protected String getReportName() {
        return "occupancy";
    }

    @Override
    protected String buildHeader() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    protected List<String> buildDataRows(String period) {
        LocalDate date = LocalDate.parse(period, DATE_FORMATTER);

        List<ReservationStatus> paidStatuses = List.of(
            ReservationStatus.CONFIRMED,
            ReservationStatus.CHECKED_IN,
            ReservationStatus.CHECKED_OUT
        );

        List<Reservation> reservations = reservationRepository.findByNightAndStatuses(date, paidStatuses);

        List<String> rows = new ArrayList<>();
        for (Reservation reservation : reservations) {
            String roomType = getRoomType(reservation.getRoomNumber());
            rows.add(String.format("%s,%s,%d,%s,%s",
                reservation.getRoomNumber(),
                roomType,
                reservation.getId(),
                reservation.getCheckIn().format(DATE_FORMATTER),
                reservation.getCheckOut().format(DATE_FORMATTER)
            ));
        }

        return rows;
    }

    @Override
    protected String buildTotal(String period) {
        LocalDate date = LocalDate.parse(period, DATE_FORMATTER);

        List<ReservationStatus> paidStatuses = List.of(
            ReservationStatus.CONFIRMED,
            ReservationStatus.CHECKED_IN,
            ReservationStatus.CHECKED_OUT
        );

        List<Reservation> reservations = reservationRepository.findByNightAndStatuses(date, paidStatuses);

        return String.format("total,%d/%d", reservations.size(), TOTAL_ROOMS);
    }

    private String getRoomType(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "";
        };
    }
}

package br.com.pousada.service;

import br.com.pousada.model.Reservation;
import br.com.pousada.model.ReservationStatus;
import br.com.pousada.model.Room;
import br.com.pousada.util.DateFormatter;
import br.com.pousada.util.MoneyFormatter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ReportService {

    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReportService(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder sb = new StringBuilder();
        sb.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> occupiedReservations = reservationService.getAllReservations().stream()
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                        r.getStatus() == ReservationStatus.CHECKED_IN ||
                        r.getStatus() == ReservationStatus.CHECKED_OUT)
            .filter(r -> isOccupiedOnDate(r, date))
            .sorted(Comparator.comparing(Reservation::getRoomNumber))
            .collect(Collectors.toList());

        for (Reservation reservation : occupiedReservations) {
            Room room = roomService.getRoomByNumber(reservation.getRoomNumber()).get();
            sb.append(reservation.getRoomNumber()).append(",");
            sb.append(room.getType()).append(",");
            sb.append(reservation.getId()).append(",");
            sb.append(DateFormatter.formatIso(reservation.getCheckIn())).append(",");
            sb.append(DateFormatter.formatIso(reservation.getCheckOut())).append("\n");
        }

        int totalRooms = roomService.getAllRooms().size();
        int occupiedRooms = occupiedReservations.size();
        sb.append("total,").append(occupiedRooms).append("/").append(totalRooms);

        return sb.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        StringBuilder sb = new StringBuilder();
        sb.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> monthReservations = reservationService.getAllReservations().stream()
            .filter(r -> YearMonth.from(r.getCheckIn()).equals(month))
            .filter(r -> r.getStatus() != ReservationStatus.PENDING)
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                             .thenComparing(Reservation::getId))
            .collect(Collectors.toList());

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Reservation reservation : monthReservations) {
            BigDecimal revenue = calculateRevenue(reservation);
            totalRevenue = totalRevenue.add(revenue);

            sb.append(reservation.getId()).append(",");
            sb.append(reservation.getRoomNumber()).append(",");
            sb.append(DateFormatter.formatIso(reservation.getCheckIn())).append(",");
            sb.append(DateFormatter.formatIso(reservation.getCheckOut())).append(",");
            sb.append(reservation.getStatus()).append(",");
            sb.append(MoneyFormatter.formatCsv(revenue)).append("\n");
        }

        sb.append("total,").append(MoneyFormatter.formatCsv(totalRevenue));

        return sb.toString();
    }

    private boolean isOccupiedOnDate(Reservation reservation, LocalDate date) {
        return !date.isBefore(reservation.getCheckIn()) && date.isBefore(reservation.getCheckOut());
    }

    private BigDecimal calculateRevenue(Reservation reservation) {
        BigDecimal paid = reservation.getAmountPaid();
        BigDecimal refunded = reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refunded);
    }
}

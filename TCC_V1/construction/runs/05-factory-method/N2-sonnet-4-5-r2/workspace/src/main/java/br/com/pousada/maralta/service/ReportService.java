package br.com.pousada.maralta.service;

import br.com.pousada.maralta.domain.Reservation;
import br.com.pousada.maralta.domain.Room;
import br.com.pousada.maralta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReportService(ReservationRepository reservationRepository, RoomService roomService) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findByDate(date);
        List<Room> allRooms = roomService.getAllRooms();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            csv.append(reservation.getRoomNumber()).append(",");

            Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
            csv.append(room.getType().name()).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getCheckIn().format(DATE_FORMAT)).append(",");
            csv.append(reservation.getCheckOut().format(DATE_FORMAT)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepository.findByMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal revenue = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                revenue = revenue.subtract(reservation.getRefund());
            }

            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(reservation.getId()).append(",");
                csv.append(reservation.getRoomNumber()).append(",");
                csv.append(reservation.getCheckIn().format(DATE_FORMAT)).append(",");
                csv.append(reservation.getCheckOut().format(DATE_FORMAT)).append(",");
                csv.append(reservation.getStatus().name()).append(",");
                csv.append(String.format(Locale.US, "%.2f", revenue)).append("\n");

                totalRevenue = totalRevenue.add(revenue);
            }
        }

        csv.append("total,").append(String.format(Locale.US, "%.2f", totalRevenue));

        return csv.toString();
    }
}

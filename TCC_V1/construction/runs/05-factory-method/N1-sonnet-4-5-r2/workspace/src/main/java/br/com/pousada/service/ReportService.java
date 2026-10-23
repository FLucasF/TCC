package br.com.pousada.service;

import br.com.pousada.domain.Reservation;
import br.com.pousada.domain.Room;
import br.com.pousada.repository.ReservationRepository;
import br.com.pousada.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findPaidByDate(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            csv.append(reservation.getRoomNumber()).append(",");

            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            csv.append(room.getType().name()).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getCheckIn().format(DATE_FORMAT)).append(",");
            csv.append(reservation.getCheckOut().format(DATE_FORMAT)).append("\n");
        }

        int occupiedCount = reservations.size();
        int totalRooms = allRooms.size();
        csv.append("total,").append(occupiedCount).append("/").append(totalRooms);

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepository.findByMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal revenue = reservation.getRevenue();
            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(reservation.getId()).append(",");
                csv.append(reservation.getRoomNumber()).append(",");
                csv.append(reservation.getCheckIn().format(DATE_FORMAT)).append(",");
                csv.append(reservation.getCheckOut().format(DATE_FORMAT)).append(",");
                csv.append(reservation.getStatus().name()).append(",");
                csv.append(formatAmount(revenue)).append("\n");

                totalRevenue = totalRevenue.add(revenue);
            }
        }

        csv.append("total,").append(formatAmount(totalRevenue));

        return csv.toString();
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}

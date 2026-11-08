package br.com.pousadamarealta.service;

import br.com.pousadamarealta.domain.Reservation;
import br.com.pousadamarealta.domain.Room;
import br.com.pousadamarealta.repository.ReservationRepository;
import br.com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public ReportService(RoomRepository roomRepository, ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Room> allRooms = roomRepository.findAll();
        List<Reservation> occupiedReservations = reservationRepository.findOccupiedRoomsOnDate(date);

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : occupiedReservations) {
            Room room = roomRepository.findByNumber(reservation.getRoomNumber());
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(room.getType().name()).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getCheckIn().format(DATE_FORMATTER)).append(",");
            csv.append(reservation.getCheckOut().format(DATE_FORMATTER)).append("\n");
        }

        csv.append("total,").append(occupiedReservations.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findReservationsStartingInMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal revenue = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                revenue = revenue.subtract(reservation.getRefund());
            }

            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(reservation.getCheckIn().format(DATE_FORMATTER)).append(",");
            csv.append(reservation.getCheckOut().format(DATE_FORMATTER)).append(",");
            csv.append(reservation.getStatus().name()).append(",");
            csv.append(String.format("%.2f", revenue)).append("\n");

            totalRevenue = totalRevenue.add(revenue);
        }

        csv.append("total,").append(String.format("%.2f", totalRevenue));

        return csv.toString();
    }
}

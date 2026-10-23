package br.com.marealto.service;

import br.com.marealto.model.Reservation;
import br.com.marealto.model.Room;
import br.com.marealto.repository.ReservationRepository;
import br.com.marealto.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public ReportService(RoomRepository roomRepository, ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findByNight(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Reservation reservation : reservations) {
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(room.getType().name()).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getCheckIn().format(formatter)).append(",");
            csv.append(reservation.getCheckOut().format(formatter)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal revenue = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                revenue = revenue.subtract(reservation.getRefund());
            }

            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(reservation.getId()).append(",");
                csv.append(reservation.getRoomNumber()).append(",");
                csv.append(reservation.getCheckIn().format(formatter)).append(",");
                csv.append(reservation.getCheckOut().format(formatter)).append(",");
                csv.append(reservation.getStatus().name()).append(",");
                csv.append(revenue.setScale(2).toPlainString()).append("\n");

                total = total.add(revenue);
            }
        }

        csv.append("total,").append(total.setScale(2).toPlainString());

        return csv.toString();
    }
}

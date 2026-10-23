package br.com.maralta.service;

import br.com.maralta.model.Reservation;
import br.com.maralta.repository.ReservationRepository;
import br.com.maralta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> reservations = reservationRepository.findByNight(date);
        int totalRooms = roomRepository.findAll().size();

        for (Reservation r : reservations) {
            csv.append(r.getRoomNumber()).append(",");

            var room = roomRepository.findByNumber(r.getRoomNumber()).orElseThrow();
            csv.append(room.getType()).append(",");
            csv.append(r.getId()).append(",");
            csv.append(r.getCheckIn().format(DATE_FORMAT)).append(",");
            csv.append(r.getCheckOut().format(DATE_FORMAT)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(totalRooms);

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> reservations = reservationRepository.findByMonth(month);
        BigDecimal total = BigDecimal.ZERO;

        for (Reservation r : reservations) {
            BigDecimal amount = r.getAmountPaid().subtract(r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO);

            csv.append(r.getId()).append(",");
            csv.append(r.getRoomNumber()).append(",");
            csv.append(r.getCheckIn().format(DATE_FORMAT)).append(",");
            csv.append(r.getCheckOut().format(DATE_FORMAT)).append(",");
            csv.append(r.getStatus()).append(",");
            csv.append(String.format("%.2f", amount)).append("\n");

            total = total.add(amount);
        }

        csv.append("total,,,,,").append(String.format("%.2f", total));

        return csv.toString();
    }
}

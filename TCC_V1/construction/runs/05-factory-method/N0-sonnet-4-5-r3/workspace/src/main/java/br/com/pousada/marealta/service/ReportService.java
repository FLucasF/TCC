package br.com.pousada.marealta.service;

import br.com.pousada.marealta.model.Reservation;
import br.com.pousada.marealta.model.Room;
import br.com.pousada.marealta.repository.ReservationRepository;
import br.com.pousada.marealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findOccupyingNight(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            csv.append(reservation.getRoomNumber()).append(",");

            Room room = allRooms.stream()
                .filter(r -> r.getNumber().equals(reservation.getRoomNumber()))
                .findFirst()
                .orElseThrow();

            csv.append(room.getType()).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getCheckIn().format(CSV_DATE_FORMAT)).append(",");
            csv.append(reservation.getCheckOut().format(CSV_DATE_FORMAT)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);

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
                csv.append(reservation.getCheckIn().format(CSV_DATE_FORMAT)).append(",");
                csv.append(reservation.getCheckOut().format(CSV_DATE_FORMAT)).append(",");
                csv.append(reservation.getStatus()).append(",");
                csv.append(formatCsvAmount(revenue)).append("\n");

                totalRevenue = totalRevenue.add(revenue);
            }
        }

        csv.append("total,").append(formatCsvAmount(totalRevenue));

        return csv.toString();
    }

    private String formatCsvAmount(BigDecimal amount) {
        return amount.toString();
    }
}

package br.com.pousadamarealta.domain.service;

import br.com.pousadamarealta.domain.model.Reservation;
import br.com.pousadamarealta.domain.model.Room;
import br.com.pousadamarealta.domain.repository.ReservationRepository;
import br.com.pousadamarealta.domain.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> reservations = reservationRepository.findByDate(date);
        List<Room> allRooms = roomRepository.findAll();

        int occupiedCount = 0;

        for (Room room : allRooms) {
            Reservation reservation = reservations.stream()
                .filter(r -> r.getRoomNumber().equals(room.getNumber()))
                .findFirst()
                .orElse(null);

            if (reservation != null) {
                csv.append(room.getNumber()).append(",")
                   .append(room.getType().name()).append(",")
                   .append(reservation.getId()).append(",")
                   .append(reservation.getCheckIn().format(DATE_FORMATTER)).append(",")
                   .append(reservation.getCheckOut().format(DATE_FORMATTER)).append("\n");
                occupiedCount++;
            }
        }

        csv.append("total,").append(occupiedCount).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> reservations = reservationRepository.findByMonth(month);
        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal amount = reservation.getAmountPaid();
            if (reservation.getRefund() != null && reservation.getRefund().compareTo(BigDecimal.ZERO) > 0) {
                amount = amount.subtract(reservation.getRefund());
            }

            csv.append(reservation.getId()).append(",")
               .append(reservation.getRoomNumber()).append(",")
               .append(reservation.getCheckIn().format(DATE_FORMATTER)).append(",")
               .append(reservation.getCheckOut().format(DATE_FORMATTER)).append(",")
               .append(reservation.getStatus().name()).append(",")
               .append(formatCsvNumber(amount)).append("\n");

            total = total.add(amount);
        }

        csv.append("total,").append(formatCsvNumber(total));

        return csv.toString();
    }

    private String formatCsvNumber(BigDecimal value) {
        return value.toString();
    }
}

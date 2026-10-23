package br.com.marealta.service;

import br.com.marealta.model.Reservation;
import br.com.marealta.model.Room;
import br.com.marealta.repository.ReservationRepository;
import br.com.marealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findByNight(date);
        int totalRooms = roomRepository.findAll().size();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(room.getType()).append(",");
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getCheckIn().format(ISO_DATE_FORMAT)).append(",");
            csv.append(reservation.getCheckOut().format(ISO_DATE_FORMAT)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(totalRooms);

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepository.findByMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal revenue = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                revenue = revenue.subtract(reservation.getRefund());
            }

            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(reservation.getCheckIn().format(ISO_DATE_FORMAT)).append(",");
            csv.append(reservation.getCheckOut().format(ISO_DATE_FORMAT)).append(",");
            csv.append(reservation.getStatus()).append(",");
            csv.append(formatDecimal(revenue)).append("\n");

            total = total.add(revenue);
        }

        csv.append("total,").append(formatDecimal(total));

        return csv.toString();
    }

    private String formatDecimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}

package com.marealta.domain.service;

import com.marealta.domain.model.Reservation;
import com.marealta.domain.model.Room;
import com.marealta.domain.repository.ReservationRepository;
import com.marealta.domain.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate night) {
        List<Reservation> reservations = reservationRepository.findByNight(night);
        List<Room> allRooms = roomRepository.findAll();

        Map<String, Room> roomMap = allRooms.stream()
            .collect(Collectors.toMap(Room::getNumber, r -> r));

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            Room room = roomMap.get(reservation.getRoomNumber());
            csv.append(reservation.getRoomNumber()).append(",")
               .append(room.getType().name()).append(",")
               .append(reservation.getId()).append(",")
               .append(reservation.getCheckIn().format(DATE_FORMAT)).append(",")
               .append(reservation.getCheckOut().format(DATE_FORMAT)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal netAmount = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                netAmount = netAmount.subtract(reservation.getRefund());
            }

            csv.append(reservation.getId()).append(",")
               .append(reservation.getRoomNumber()).append(",")
               .append(reservation.getCheckIn().format(DATE_FORMAT)).append(",")
               .append(reservation.getCheckOut().format(DATE_FORMAT)).append(",")
               .append(reservation.getStatus().name()).append(",")
               .append(formatAmount(netAmount)).append("\n");

            total = total.add(netAmount);
        }

        csv.append("total,").append(formatAmount(total));

        return csv.toString();
    }

    private String formatAmount(BigDecimal amount) {
        return String.format(Locale.US, "%.2f", amount);
    }
}

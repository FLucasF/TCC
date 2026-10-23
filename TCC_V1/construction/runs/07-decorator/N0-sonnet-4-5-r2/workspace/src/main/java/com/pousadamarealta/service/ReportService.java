package com.pousadamarealta.service;

import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.domain.repository.ReservationRepository;
import com.pousadamarealta.domain.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        List<Reservation> reservations = reservationRepository.findPaidReservationsForNight(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : reservations) {
            Room room = roomRepository.findByNumber(r.getRoomNumber()).orElseThrow();
            csv.append(r.getRoomNumber()).append(",");
            csv.append(room.getType()).append(",");
            csv.append(r.getId()).append(",");
            csv.append(r.getCheckIn().format(DATE_FORMAT)).append(",");
            csv.append(r.getCheckOut().format(DATE_FORMAT)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepository.findReservationsStartingInMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation r : reservations) {
            BigDecimal revenue = r.getAmountPaid().subtract(
                r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO
            );

            csv.append(r.getId()).append(",");
            csv.append(r.getRoomNumber()).append(",");
            csv.append(r.getCheckIn().format(DATE_FORMAT)).append(",");
            csv.append(r.getCheckOut().format(DATE_FORMAT)).append(",");
            csv.append(r.getStatus()).append(",");
            csv.append(formatDecimal(revenue)).append("\n");

            total = total.add(revenue);
        }

        csv.append("total,").append(formatDecimal(total));

        return csv.toString();
    }

    private String formatDecimal(BigDecimal value) {
        return value.toString();
    }
}

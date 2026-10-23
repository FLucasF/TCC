package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> occupiedReservations = reservationRepository.findPaidByDate(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Reservation reservation : occupiedReservations) {
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                reservation.getRoomNumber(),
                getRoomType(reservation.getRoomNumber()),
                reservation.getId(),
                reservation.getCheckIn().format(formatter),
                reservation.getCheckOut().format(formatter)
            ));
        }

        csv.append(String.format("total,%d/%d\n", occupiedReservations.size(), allRooms.size()));

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findRevenueByMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal revenue = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                revenue = revenue.subtract(reservation.getRefund());
            }

            csv.append(String.format("%d,%s,%s,%s,%s,%s\n",
                reservation.getId(),
                reservation.getRoomNumber(),
                reservation.getCheckIn().format(formatter),
                reservation.getCheckOut().format(formatter),
                reservation.getStatus(),
                formatCsvMoney(revenue)
            ));

            total = total.add(revenue);
        }

        csv.append(String.format("total,%s\n", formatCsvMoney(total)));

        return csv.toString();
    }

    private String getRoomType(String roomNumber) {
        return roomRepository.findByNumber(roomNumber)
            .map(room -> room.getType().name())
            .orElse("UNKNOWN");
    }

    private String formatCsvMoney(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}

package com.pousadamarealta.domain.service;

import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate night) throws SQLException {
        List<Reservation> reservations = reservationRepository.findByNight(night);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                reservation.getRoomNumber(),
                getRoomType(reservation.getRoomNumber()),
                reservation.getId(),
                reservation.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE),
                reservation.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE)
            ));
        }

        csv.append(String.format("total,%d/%d\n", reservations.size(), allRooms.size()));

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) throws SQLException {
        List<Reservation> reservations = reservationRepository.findByMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal revenue = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                revenue = revenue.subtract(reservation.getRefund());
            }

            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(String.format("%d,%s,%s,%s,%s,%s\n",
                    reservation.getId(),
                    reservation.getRoomNumber(),
                    reservation.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    reservation.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    reservation.getStatus().name(),
                    formatDecimal(revenue)
                ));
                total = total.add(revenue);
            }
        }

        csv.append(String.format("total,%s\n", formatDecimal(total)));

        return csv.toString();
    }

    private String getRoomType(String roomNumber) throws SQLException {
        return roomRepository.findByNumber(roomNumber)
            .map(room -> room.getType().name())
            .orElse("UNKNOWN");
    }

    private String formatDecimal(BigDecimal value) {
        return value.toString();
    }
}

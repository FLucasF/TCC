package com.pousada.service;

import com.pousada.domain.Money;
import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.repository.ReservationRepository;
import com.pousada.util.DateFormatter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomService roomService;

    public ReportService(ReservationRepository reservationRepository, RoomService roomService) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findPaidByNight(date);
        List<Room> allRooms = roomService.getAllRooms();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
            csv.append(room.getNumber()).append(",")
               .append(room.getType()).append(",")
               .append(reservation.getId()).append(",")
               .append(DateFormatter.formatISO(reservation.getCheckIn())).append(",")
               .append(DateFormatter.formatISO(reservation.getCheckOut())).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        Money totalRevenue = new Money(BigDecimal.ZERO);

        for (Reservation reservation : reservations) {
            Money revenue = calculateRevenue(reservation);
            totalRevenue = totalRevenue.add(revenue);

            csv.append(reservation.getId()).append(",")
               .append(reservation.getRoomNumber()).append(",")
               .append(DateFormatter.formatISO(reservation.getCheckIn())).append(",")
               .append(DateFormatter.formatISO(reservation.getCheckOut())).append(",")
               .append(reservation.getStatus()).append(",")
               .append(revenue.getAmount()).append("\n");
        }

        csv.append("total,").append(totalRevenue.getAmount());

        return csv.toString();
    }

    private Money calculateRevenue(Reservation reservation) {
        Money paid = reservation.getAmountPaid();
        Money refunded = reservation.getRefund() != null ? reservation.getRefund() : new Money(BigDecimal.ZERO);
        return paid.subtract(refunded);
    }
}

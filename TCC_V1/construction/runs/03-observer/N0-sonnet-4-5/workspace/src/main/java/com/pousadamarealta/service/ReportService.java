package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findByNight(date);
        long totalRooms = roomRepository.findAll().size();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                    reservation.getRoomNumber(),
                    reservation.getRatePlan(),
                    reservation.getId(),
                    reservation.getCheckIn().format(DATE_FORMATTER),
                    reservation.getCheckOut().format(DATE_FORMATTER)));
        }

        csv.append(String.format("total,%d/%d", reservations.size(), totalRooms));

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal amount = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                amount = amount.subtract(reservation.getRefund());
            }
            total = total.add(amount);

            csv.append(String.format(Locale.US, "%d,%s,%s,%s,%s,%.2f\n",
                    reservation.getId(),
                    reservation.getRoomNumber(),
                    reservation.getCheckIn().format(DATE_FORMATTER),
                    reservation.getCheckOut().format(DATE_FORMATTER),
                    reservation.getStatus(),
                    amount));
        }

        csv.append(String.format(Locale.US, "total,%.2f", total));

        return csv.toString();
    }
}

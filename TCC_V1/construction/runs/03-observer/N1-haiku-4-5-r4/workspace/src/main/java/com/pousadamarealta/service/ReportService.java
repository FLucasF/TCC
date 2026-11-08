package com.pousadamarealta.service;

import com.pousadamarealta.models.Reservation;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.utils.FormatterUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) throws Exception {
        List<Reservation> reservations = reservationRepository.findOccupancyByDate(date);
        List<?> rooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation res : reservations) {
            csv.append(res.roomNumber).append(",")
                .append(res.roomType).append(",")
                .append(res.id).append(",")
                .append(FormatterUtils.formatCsvDate(res.checkIn)).append(",")
                .append(FormatterUtils.formatCsvDate(res.checkOut)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(rooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) throws Exception {
        List<Reservation> reservations = reservationRepository.findRevenueByMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal amount = res.amountPaid.subtract(
                res.refund != null ? res.refund : BigDecimal.ZERO
            );

            csv.append(res.id).append(",")
                .append(res.roomNumber).append(",")
                .append(FormatterUtils.formatCsvDate(res.checkIn)).append(",")
                .append(FormatterUtils.formatCsvDate(res.checkOut)).append(",")
                .append(res.status).append(",")
                .append(FormatterUtils.formatCsvNumber(amount)).append("\n");

            total = total.add(amount);
        }

        csv.append("total,,,,,").append(FormatterUtils.formatCsvNumber(total));

        return csv.toString();
    }
}

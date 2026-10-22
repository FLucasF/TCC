package com.pousada.marealta.service;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.infrastructure.persistence.ReservationRepository;
import com.pousada.marealta.infrastructure.persistence.RoomRepository;
import com.pousada.marealta.util.DateFormatter;
import com.pousada.marealta.util.MoneyFormatter;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) throws Exception {
        List<Reservation> occupancy = reservationRepository.findOccupancyByDate(date);

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation res : occupancy) {
            var room = roomRepository.findByNumber(res.getRoomNumber());
            csv.append(res.getRoomNumber()).append(",");
            csv.append(room.getType().getName()).append(",");
            csv.append(res.getId()).append(",");
            csv.append(DateFormatter.toISO(res.getCheckIn())).append(",");
            csv.append(DateFormatter.toISO(res.getCheckOut())).append("\n");
        }

        csv.append("total,").append(occupancy.size()).append("/5\n");

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) throws Exception {
        List<Reservation> reservations = reservationRepository.findRevenueByMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        double totalAmount = 0;
        for (Reservation res : reservations) {
            csv.append(res.getId()).append(",");
            csv.append(res.getRoomNumber()).append(",");
            csv.append(DateFormatter.toISO(res.getCheckIn())).append(",");
            csv.append(DateFormatter.toISO(res.getCheckOut())).append(",");
            csv.append(res.getStatus().name()).append(",");

            double amount = res.getAmountPaid();
            if (res.getRefund() != null) {
                amount -= res.getRefund();
            }

            csv.append(MoneyFormatter.formatForCSV(amount)).append("\n");
            totalAmount += amount;
        }

        csv.append("total,,,,,").append(MoneyFormatter.formatForCSV(totalAmount)).append("\n");

        return csv.toString();
    }
}

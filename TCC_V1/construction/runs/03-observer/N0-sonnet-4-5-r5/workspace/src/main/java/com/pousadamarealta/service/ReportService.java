package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findByNight(date);
        int totalRooms = roomRepository.findAll().size();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : reservations) {
            csv.append(r.getRoomNumber()).append(",");
            csv.append(getRoomTypeName(r.getRoomNumber())).append(",");
            csv.append(r.getId()).append(",");
            csv.append(r.getCheckIn().format(dateFormatter)).append(",");
            csv.append(r.getCheckOut().format(dateFormatter)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(totalRooms);

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepository.findByMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation r : reservations) {
            BigDecimal netAmount = r.getAmountPaid();
            if (r.getRefund() != null) {
                netAmount = netAmount.subtract(r.getRefund());
            }

            if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(r.getId()).append(",");
                csv.append(r.getRoomNumber()).append(",");
                csv.append(r.getCheckIn().format(dateFormatter)).append(",");
                csv.append(r.getCheckOut().format(dateFormatter)).append(",");
                csv.append(r.getStatus()).append(",");
                csv.append(formatAmount(netAmount)).append("\n");

                total = total.add(netAmount);
            }
        }

        csv.append("total,").append(formatAmount(total));

        return csv.toString();
    }

    private String getRoomTypeName(String roomNumber) {
        if (roomNumber.startsWith("1")) return "STANDARD";
        if (roomNumber.startsWith("2")) return "DELUXE";
        return "SUITE";
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString();
    }
}

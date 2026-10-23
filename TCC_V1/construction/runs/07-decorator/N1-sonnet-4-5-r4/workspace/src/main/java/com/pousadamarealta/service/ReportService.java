package com.pousadamarealta.service;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReportService {
    private final ReservationService reservationService;
    private final RoomRepository roomRepository;

    public ReportService(ReservationService reservationService, RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationService.findByDate(date);
        StringBuilder csv = new StringBuilder();

        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : reservations) {
            Room room = roomRepository.findByNumber(r.getRoomNumber()).get();
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                    r.getRoomNumber(),
                    room.getType().name(),
                    r.getId(),
                    r.getCheckIn(),
                    r.getCheckOut()));
        }

        int totalRooms = roomRepository.findAll().size();
        csv.append(String.format("total,%d/%d\n", reservations.size(), totalRooms));

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationService.findByMonth(year, month);
        StringBuilder csv = new StringBuilder();

        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation r : reservations) {
            BigDecimal revenue = r.getAmountPaid();
            if (r.getRefund() != null) {
                revenue = revenue.subtract(r.getRefund());
            }

            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(String.format("%d,%s,%s,%s,%s,%s\n",
                        r.getId(),
                        r.getRoomNumber(),
                        r.getCheckIn(),
                        r.getCheckOut(),
                        r.getStatus().name(),
                        formatCsvAmount(revenue)));

                total = total.add(revenue);
            }
        }

        csv.append(String.format("total,%s\n", formatCsvAmount(total)));

        return csv.toString();
    }

    private String formatCsvAmount(BigDecimal amount) {
        return amount.toString();
    }
}

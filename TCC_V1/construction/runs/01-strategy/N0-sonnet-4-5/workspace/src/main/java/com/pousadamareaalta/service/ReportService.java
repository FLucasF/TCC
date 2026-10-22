package com.pousadamareaalta.service;

import com.pousadamareaalta.model.Reservation;
import com.pousadamareaalta.model.Room;
import com.pousadamareaalta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomService roomService;

    public ReportService(ReservationRepository reservationRepository, RoomService roomService) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findByDateAndStatus(date);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : reservations) {
            Room room = roomService.getRoom(r.getRoomNumber());
            csv.append(String.format("%d,%s,%d,%s,%s\n",
                r.getRoomNumber(),
                room.getType().name(),
                r.getId(),
                r.getCheckIn().format(formatter),
                r.getCheckOut().format(formatter)));
        }

        int totalRooms = roomService.getAllRooms().size();
        csv.append(String.format("total,%d/%d\n", reservations.size(), totalRooms));

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepository.findByMonth(year, month);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation r : reservations) {
            BigDecimal netAmount = r.getAmountPaid();
            if (r.getRefund() != null) {
                netAmount = netAmount.subtract(r.getRefund());
            }

            if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(String.format("%d,%d,%s,%s,%s,%s\n",
                    r.getId(),
                    r.getRoomNumber(),
                    r.getCheckIn().format(formatter),
                    r.getCheckOut().format(formatter),
                    r.getStatus().name(),
                    formatCsvMoney(netAmount)));

                total = total.add(netAmount);
            }
        }

        csv.append(String.format("total,%s\n", formatCsvMoney(total)));

        return csv.toString();
    }

    private String formatCsvMoney(BigDecimal amount) {
        return String.format("%.2f", amount);
    }
}

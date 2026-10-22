package com.pousada.mare_alta.service;

import com.pousada.mare_alta.model.Reservation;
import com.pousada.mare_alta.model.Room;
import com.pousada.mare_alta.repository.ReservationRepository;
import com.pousada.mare_alta.repository.RoomRepository;
import com.pousada.mare_alta.util.NumberFormatter;
import java.math.BigDecimal;
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

    public String generateOccupancyReport(LocalDate date) throws Exception {
        List<Reservation> reservations = reservationRepository.findPaidByNightOrderedByRoom(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Room room : allRooms) {
            boolean found = false;
            for (Reservation res : reservations) {
                if (res.getRoomNumber() == room.getNumber()) {
                    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    csv.append(room.getNumber()).append(",");
                    csv.append(room.getType().getValue()).append(",");
                    csv.append(res.getId()).append(",");
                    csv.append(res.getCheckIn().format(dateFormatter)).append(",");
                    csv.append(res.getCheckOut().format(dateFormatter)).append("\n");
                    found = true;
                    break;
                }
            }
        }

        int occupiedRooms = (int) reservations.stream()
                .map(Reservation::getRoomNumber)
                .distinct()
                .count();

        csv.append("total,").append(occupiedRooms).append("/").append(allRooms.size()).append("\n");

        return csv.toString();
    }

    public String generateRevenueReport(String yearMonth) throws Exception {
        YearMonth ym = YearMonth.parse(yearMonth);
        List<Reservation> reservations = reservationRepository.findPaidByMonthOrderedByDate(
                ym.getYear(), ym.getMonthValue());

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalAmount = BigDecimal.ZERO;
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Reservation res : reservations) {
            csv.append(res.getId()).append(",");
            csv.append(res.getRoomNumber()).append(",");
            csv.append(res.getCheckIn().format(dateFormatter)).append(",");
            csv.append(res.getCheckOut().format(dateFormatter)).append(",");
            csv.append(res.getStatus().getValue()).append(",");

            BigDecimal amount = calculateRevenueAmount(res);
            csv.append(NumberFormatter.formatCurrencyForCsv(amount)).append("\n");
            totalAmount = totalAmount.add(amount);
        }

        csv.append("total,").append(NumberFormatter.formatCurrencyForCsv(totalAmount)).append("\n");

        return csv.toString();
    }

    private BigDecimal calculateRevenueAmount(Reservation res) {
        BigDecimal amount = res.getAmountPaid();
        if (res.getRefund() != null && res.getRefund().signum() > 0) {
            amount = amount.subtract(res.getRefund());
        }
        return amount;
    }
}

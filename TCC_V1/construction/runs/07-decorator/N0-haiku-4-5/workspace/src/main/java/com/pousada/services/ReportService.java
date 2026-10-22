package com.pousada.services;

import com.pousada.models.Reservation;
import com.pousada.models.Room;
import com.pousada.repositories.ReservationRepository;
import com.pousada.repositories.RoomRepository;
import com.pousada.utils.FormatterUtil;
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

    public String getOccupancyReport(LocalDate date) throws Exception {
        List<Room> allRooms = roomRepository.findAll();
        StringBuilder csv = new StringBuilder();

        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedCount = 0;

        for (Room room : allRooms) {
            List<Reservation> reservations = reservationRepository.findByRoomAndNight(room.getNumber(), date);

            if (reservations.isEmpty()) {
                continue;
            }

            for (Reservation res : reservations) {
                csv.append(res.getRoomNumber()).append(",");
                csv.append(room.getType()).append(",");
                csv.append(res.getId()).append(",");
                csv.append(FormatterUtil.formatDateISO(res.getCheckIn())).append(",");
                csv.append(FormatterUtil.formatDateISO(res.getCheckOut())).append("\n");
            }

            occupiedCount++;
        }

        csv.append("total,").append(occupiedCount).append("/").append(allRooms.size()).append("\n");

        return csv.toString();
    }

    public String getRevenueReport(int year, int month) throws Exception {
        List<Reservation> reservations = reservationRepository.findByMonthAndNotCancelled(year, month);
        StringBuilder csv = new StringBuilder();

        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal revenue = res.getAmountPaid();
            if (res.getRefund() != null) {
                revenue = revenue.subtract(res.getRefund());
            }

            csv.append(res.getId()).append(",");
            csv.append(res.getRoomNumber()).append(",");
            csv.append(FormatterUtil.formatDateISO(res.getCheckIn())).append(",");
            csv.append(FormatterUtil.formatDateISO(res.getCheckOut())).append(",");
            csv.append(res.getStatus()).append(",");
            csv.append(FormatterUtil.formatCurrencyCSV(revenue)).append("\n");

            totalAmount = totalAmount.add(revenue);
        }

        csv.append("total,").append(FormatterUtil.formatCurrencyCSV(totalAmount)).append("\n");

        return csv.toString();
    }
}

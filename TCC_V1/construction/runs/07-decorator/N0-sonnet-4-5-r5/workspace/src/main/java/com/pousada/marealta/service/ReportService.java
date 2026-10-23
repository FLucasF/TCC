package com.pousada.marealta.service;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.Room;
import com.pousada.marealta.repository.ReservationRepository;
import com.pousada.marealta.repository.RoomRepository;

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

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findByNight(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : reservations) {
            Room room = roomRepository.findByNumber(r.getRoomNumber());
            csv.append(r.getRoomNumber()).append(",");
            csv.append(room.getType()).append(",");
            csv.append(r.getId()).append(",");
            csv.append(r.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(",");
            csv.append(r.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation r : reservations) {
            // Dinheiro que ficou com a pousada = pago - reembolsado
            BigDecimal amount = r.getAmountPaid();
            if (r.getRefund() != null) {
                amount = amount.subtract(r.getRefund());
            }

            // Só conta se ficou algum dinheiro
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(r.getId()).append(",");
                csv.append(r.getRoomNumber()).append(",");
                csv.append(r.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(",");
                csv.append(r.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(",");
                csv.append(r.getStatus()).append(",");
                csv.append(formatCsvMoney(amount)).append("\n");

                total = total.add(amount);
            }
        }

        csv.append("total,").append(formatCsvMoney(total));

        return csv.toString();
    }

    private String formatCsvMoney(BigDecimal amount) {
        return amount.toString();
    }
}

package com.pousadamarealta.domain;

import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> occupied = reservationRepository.findOccupiedOnDate(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        occupied.stream()
                .sorted(Comparator.comparingInt(Reservation::getRoomNumber))
                .forEach(r -> {
                    Room room = roomRepository.findByNumber(r.getRoomNumber()).orElseThrow();
                    csv.append(r.getRoomNumber()).append(",");
                    csv.append(room.getType().name()).append(",");
                    csv.append(r.getId()).append(",");
                    csv.append(r.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(",");
                    csv.append(r.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n");
                });

        csv.append("total,").append(occupied.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationRepository.findStartingInMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalRevenue = BigDecimal.ZERO;

        List<Reservation> sorted = reservations.stream()
                .sorted(Comparator.comparing(Reservation::getCheckIn)
                        .thenComparing(Reservation::getId))
                .toList();

        for (Reservation r : sorted) {
            Room room = roomRepository.findByNumber(r.getRoomNumber()).orElseThrow();
            BigDecimal revenue = r.getAmountPaid();
            if (r.getRefund() != null) {
                revenue = revenue.subtract(r.getRefund());
            }

            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(r.getId()).append(",");
                csv.append(r.getRoomNumber()).append(",");
                csv.append(r.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(",");
                csv.append(r.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(",");
                csv.append(r.getStatus().name()).append(",");
                csv.append(String.format("%.2f", revenue.doubleValue())).append("\n");

                totalRevenue = totalRevenue.add(revenue);
            }
        }

        csv.append("total,").append(String.format("%.2f", totalRevenue.doubleValue()));

        return csv.toString();
    }
}

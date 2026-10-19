package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.repository.InMemoryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final InMemoryRepository repository = InMemoryRepository.getInstance();

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Room> rooms = new ArrayList<>(repository.getAllRooms());
        rooms.sort(Comparator.comparingInt(Room::getNumber));

        int occupiedRooms = 0;

        for (Room room : rooms) {
            List<Reservation> reservations = repository.getReservationsByRoomAndDate(room.getNumber(), date);
            if (!reservations.isEmpty()) {
                for (Reservation res : reservations) {
                    csv.append(String.format("%d,%s,%d,%s,%s\n",
                        room.getNumber(),
                        room.getType(),
                        res.getId(),
                        res.getCheckIn().format(DATE_FORMAT),
                        res.getCheckOut().format(DATE_FORMAT)
                    ));
                    occupiedRooms++;
                }
            }
        }

        csv.append(String.format("total,%d/%d\n", occupiedRooms, rooms.size()));

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        // Get all reservations for the month, including cancelled ones
        List<Reservation> allReservations = new ArrayList<>(repository.getAllReservations());
        List<Reservation> reservations = allReservations.stream()
            .filter(r -> r.getCheckIn().getYear() == year && r.getCheckIn().getMonthValue() == month)
            .filter(r -> r.getStatus() == Reservation.Status.CONFIRMED ||
                         r.getStatus() == Reservation.Status.CHECKED_IN ||
                         r.getStatus() == Reservation.Status.CHECKED_OUT ||
                         r.getStatus() == Reservation.Status.CANCELLED)
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                .thenComparingLong(Reservation::getId))
            .toList();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal amount = calculateMoneyRetained(res);
            totalAmount = totalAmount.add(amount);

            csv.append(String.format("%d,%d,%s,%s,%s,%s\n",
                res.getId(),
                res.getRoomNumber(),
                res.getCheckIn().format(DATE_FORMAT),
                res.getCheckOut().format(DATE_FORMAT),
                res.getStatus(),
                formatCsvPrice(amount)
            ));
        }

        csv.append(String.format("total,%.2f\n", totalAmount));

        return csv.toString();
    }

    private BigDecimal calculateMoneyRetained(Reservation reservation) {
        BigDecimal retained = reservation.getAmountPaid();

        if (reservation.getStatus() == Reservation.Status.CANCELLED) {
            BigDecimal refunded = reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO;
            retained = retained.subtract(refunded);
        }

        return retained;
    }

    private String formatCsvPrice(BigDecimal amount) {
        return String.format("%.2f", amount);
    }
}

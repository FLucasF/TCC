package com.pousadamarealta.domain.service;

import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.ReservationStatus;
import com.pousadamarealta.domain.repository.ReservationRepository;
import com.pousadamarealta.domain.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> occupiedReservations = reservationRepository.findAll().stream()
            .filter(r -> r.getStatus() != ReservationStatus.PENDING)
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
            .filter(r -> r.occupiesNight(date))
            .sorted(Comparator.comparing(Reservation::getRoomNumber))
            .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : occupiedReservations) {
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                r.getRoomNumber(),
                getRoomTypeName(r.getRoomNumber()),
                r.getId(),
                r.getCheckIn().format(DATE_FORMATTER),
                r.getCheckOut().format(DATE_FORMATTER)
            ));
        }

        int totalRooms = roomRepository.findAll().size();
        csv.append(String.format("total,%d/%d\n", occupiedReservations.size(), totalRooms));

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> relevantReservations = reservationRepository.findAll().stream()
            .filter(r -> r.startsInMonth(year, month))
            .filter(r -> r.getRevenueAmount().compareTo(BigDecimal.ZERO) > 0)
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                .thenComparing(Reservation::getId))
            .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation r : relevantReservations) {
            BigDecimal amount = r.getRevenueAmount();
            csv.append(String.format("%d,%s,%s,%s,%s,%s\n",
                r.getId(),
                r.getRoomNumber(),
                r.getCheckIn().format(DATE_FORMATTER),
                r.getCheckOut().format(DATE_FORMATTER),
                r.getStatus(),
                formatCsvAmount(amount)
            ));
            total = total.add(amount);
        }

        csv.append(String.format("total,%s\n", formatCsvAmount(total)));

        return csv.toString();
    }

    private String getRoomTypeName(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "";
        };
    }

    private String formatCsvAmount(BigDecimal amount) {
        return amount.toPlainString();
    }
}

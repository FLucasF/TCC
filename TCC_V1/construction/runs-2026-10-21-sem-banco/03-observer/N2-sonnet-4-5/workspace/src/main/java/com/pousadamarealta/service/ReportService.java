package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public final class ReportService {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public ReportService(RoomRepository roomRepository, ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> occupiedReservations = reservationRepository.findAll().stream()
            .filter(r -> r.isPaid())
            .filter(r -> !r.checkIn().isAfter(date) && r.checkOut().isAfter(date))
            .sorted(Comparator.comparingInt(Reservation::roomNumber))
            .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : occupiedReservations) {
            csv.append(String.format("%d,%s,%d,%s,%s\n",
                r.roomNumber(),
                r.roomType(),
                r.id(),
                formatDate(r.checkIn()),
                formatDate(r.checkOut())
            ));
        }

        int totalRooms = roomRepository.findAll().size();
        csv.append(String.format("total,%d/%d\n", occupiedReservations.size(), totalRooms));

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findAll().stream()
            .filter(r -> YearMonth.from(r.checkIn()).equals(month))
            .filter(r -> r.revenue().compareTo(BigDecimal.ZERO) > 0)
            .sorted(Comparator.comparing(Reservation::checkIn).thenComparingInt(Reservation::id))
            .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation r : reservations) {
            csv.append(String.format("%d,%d,%s,%s,%s,%s\n",
                r.id(),
                r.roomNumber(),
                formatDate(r.checkIn()),
                formatDate(r.checkOut()),
                r.status(),
                formatAmount(r.revenue())
            ));
            total = total.add(r.revenue());
        }

        csv.append(String.format("total,%s\n", formatAmount(total)));

        return csv.toString();
    }

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String formatAmount(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }
}

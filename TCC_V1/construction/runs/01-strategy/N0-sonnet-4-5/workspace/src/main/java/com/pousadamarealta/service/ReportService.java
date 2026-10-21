package com.pousadamarealta.service;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
                .filter(r -> isOccupiedOnDate(r, date))
                .sorted(Comparator.comparing(Reservation::getRoomNumber))
                .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : occupiedReservations) {
            String roomType = roomRepository.findByNumber(reservation.getRoomNumber())
                    .map(r -> r.getType().name())
                    .orElse("");

            csv.append(String.format("%s,%s,%d,%s,%s\n",
                    reservation.getRoomNumber(),
                    roomType,
                    reservation.getId(),
                    reservation.getCheckIn().format(DATE_FORMATTER),
                    reservation.getCheckOut().format(DATE_FORMATTER)));
        }

        int totalRooms = roomRepository.findAll().size();
        csv.append(String.format("total,%d/%d", occupiedReservations.size(), totalRooms));

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> monthReservations = reservationRepository.findAll().stream()
                .filter(r -> YearMonth.from(r.getCheckIn()).equals(month))
                .filter(r -> hasRevenue(r))
                .sorted(Comparator.comparing(Reservation::getCheckIn)
                        .thenComparing(Reservation::getId))
                .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : monthReservations) {
            BigDecimal revenue = calculateRevenue(reservation);
            total = total.add(revenue);

            csv.append(String.format("%d,%s,%s,%s,%s,%s\n",
                    reservation.getId(),
                    reservation.getRoomNumber(),
                    reservation.getCheckIn().format(DATE_FORMATTER),
                    reservation.getCheckOut().format(DATE_FORMATTER),
                    reservation.getStatus().name(),
                    formatAmount(revenue)));
        }

        csv.append(String.format("total,%s", formatAmount(total)));

        return csv.toString();
    }

    private boolean isOccupiedOnDate(Reservation reservation, LocalDate date) {
        return !date.isBefore(reservation.getCheckIn()) && date.isBefore(reservation.getCheckOut());
    }

    private boolean hasRevenue(Reservation reservation) {
        BigDecimal revenue = calculateRevenue(reservation);
        return revenue.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal calculateRevenue(Reservation reservation) {
        BigDecimal paid = reservation.getAmountPaid();
        BigDecimal refunded = reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refunded);
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2).toPlainString();
    }
}

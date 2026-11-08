package com.pousada.marealba.service;

import com.pousada.marealba.model.*;
import com.pousada.marealba.repository.ReservationRepository;
import com.pousada.marealba.util.DateUtil;
import com.pousada.marealba.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ReportService {
    private static final BigDecimal TOURISM_TAX_PER_GUEST_NIGHT = new BigDecimal("4.50");

    private final ReservationRepository reservationRepository;
    private final RoomService roomService;

    public ReportService(ReservationRepository reservationRepository, RoomService roomService) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();

        csv.append("Pousada Maré Alta,occupancy,").append(DateUtil.formatIso(date)).append("\n");
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Room> allRooms = roomService.getAllRooms();
        int occupiedCount = 0;

        for (Room room : allRooms) {
            List<Reservation> reservations = reservationRepository.findByRoomAndDate(room.getNumber(), date);

            if (!reservations.isEmpty()) {
                Reservation reservation = reservations.get(0);
                csv.append(room.getNumber()).append(",")
                   .append(room.getType().name()).append(",")
                   .append(reservation.getId()).append(",")
                   .append(DateUtil.formatIso(reservation.getCheckIn())).append(",")
                   .append(DateUtil.formatIso(reservation.getCheckOut())).append("\n");
                occupiedCount++;
            }
        }

        csv.append("total,").append(occupiedCount).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        StringBuilder csv = new StringBuilder();

        csv.append("Pousada Maré Alta,revenue,")
           .append(String.format("%04d-%02d", year, month)).append("\n");
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> reservations = reservationRepository.findByCheckInMonthWithRevenue(year, month);
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal revenue = calculateRevenue(reservation);
            totalRevenue = totalRevenue.add(revenue);

            csv.append(reservation.getId()).append(",")
               .append(reservation.getRoomNumber()).append(",")
               .append(DateUtil.formatIso(reservation.getCheckIn())).append(",")
               .append(DateUtil.formatIso(reservation.getCheckOut())).append(",")
               .append(reservation.getStatus().name()).append(",")
               .append(MoneyUtil.formatCsv(revenue)).append("\n");
        }

        csv.append("total,").append(MoneyUtil.formatCsv(totalRevenue));

        return csv.toString();
    }

    public String generateTourismTaxReport(int year, int month) {
        StringBuilder csv = new StringBuilder();

        csv.append("Pousada Maré Alta,tourism-tax,")
           .append(String.format("%04d-%02d", year, month)).append("\n");
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        List<Reservation> reservations = reservationRepository.findPaidByCheckInMonth(year, month);
        BigDecimal totalTax = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal tax = calculateTourismTax(reservation);
            totalTax = totalTax.add(tax);

            csv.append(reservation.getId()).append(",")
               .append(reservation.getRoomNumber()).append(",")
               .append(DateUtil.formatIso(reservation.getCheckIn())).append(",")
               .append(DateUtil.formatIso(reservation.getCheckOut())).append(",")
               .append(reservation.getGuests()).append(",")
               .append(reservation.getNights()).append(",")
               .append(MoneyUtil.formatCsv(tax)).append("\n");
        }

        csv.append("total,").append(MoneyUtil.formatCsv(totalTax));

        return csv.toString();
    }

    public String generateRatePlansReport(int year, int month) {
        StringBuilder csv = new StringBuilder();

        csv.append("Pousada Maré Alta,rate-plans,")
           .append(String.format("%04d-%02d", year, month)).append("\n");
        csv.append("rate_plan,reservations,amount\n");

        List<Reservation> allReservations = reservationRepository.findByCheckInMonthWithRevenue(year, month);

        Map<RatePlan, List<Reservation>> byRatePlan = allReservations.stream()
            .collect(Collectors.groupingBy(Reservation::getRatePlan));

        int totalReservations = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> reservations = byRatePlan.getOrDefault(plan, Collections.emptyList());
            int count = reservations.size();

            BigDecimal amount = reservations.stream()
                .map(this::calculateRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            csv.append(plan.name()).append(",")
               .append(count).append(",")
               .append(MoneyUtil.formatCsv(amount)).append("\n");

            totalReservations += count;
            totalAmount = totalAmount.add(amount);
        }

        csv.append("total,").append(totalReservations).append(",")
           .append(MoneyUtil.formatCsv(totalAmount));

        return csv.toString();
    }

    private BigDecimal calculateRevenue(Reservation reservation) {
        BigDecimal revenue = reservation.getAmountPaid();
        if (reservation.getRefund() != null) {
            revenue = revenue.subtract(reservation.getRefund());
        }
        return revenue;
    }

    private BigDecimal calculateTourismTax(Reservation reservation) {
        int guestNights = reservation.getGuests() * reservation.getNights();
        return TOURISM_TAX_PER_GUEST_NIGHT.multiply(BigDecimal.valueOf(guestNights));
    }
}

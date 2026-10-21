package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportService {
    private static final String POUSADA_NAME = "Pousada Maré Alta";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final PricingService pricingService;

    public ReportService(ReservationRepository reservationRepository,
                         RoomRepository roomRepository,
                         PricingService pricingService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.pricingService = pricingService;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> occupiedReservations = reservationRepository.findPaidReservationsForNight(date)
                .stream()
                .sorted(Comparator.comparing(Reservation::getRoomNumber))
                .toList();

        int totalRooms = roomRepository.findAll().size();
        int occupiedRooms = occupiedReservations.size();

        ReportBuilder builder = new ReportBuilder();
        builder.addTitleLine(POUSADA_NAME, "occupancy", date.format(DATE_FORMATTER));
        builder.addHeaderLine("room", "type", "reservation", "check_in", "check_out");

        for (Reservation reservation : occupiedReservations) {
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            builder.addLine(
                    reservation.getRoomNumber(),
                    room.getType().toString(),
                    String.valueOf(reservation.getId()),
                    reservation.getCheckIn().format(DATE_FORMATTER),
                    reservation.getCheckOut().format(DATE_FORMATTER)
            );
        }

        builder.addTotalLine("total", occupiedRooms + "/" + totalRooms);

        return builder.build();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findReservationsStartingInMonth(month)
                .stream()
                .filter(r -> r.getNetAmount().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(Reservation::getCheckIn)
                        .thenComparing(Reservation::getId))
                .toList();

        BigDecimal totalRevenue = reservations.stream()
                .map(Reservation::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ReportBuilder builder = new ReportBuilder();
        builder.addTitleLine(POUSADA_NAME, "revenue", month.toString());
        builder.addHeaderLine("reservation", "room", "check_in", "check_out", "status", "amount");

        for (Reservation reservation : reservations) {
            builder.addLine(
                    String.valueOf(reservation.getId()),
                    reservation.getRoomNumber(),
                    reservation.getCheckIn().format(DATE_FORMATTER),
                    reservation.getCheckOut().format(DATE_FORMATTER),
                    reservation.getStatus().toString(),
                    formatDecimal(reservation.getNetAmount())
            );
        }

        builder.addTotalLine("total", formatDecimal(totalRevenue));

        return builder.build();
    }

    public String generateTourismTaxReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findReservationsStartingInMonth(month)
                .stream()
                .filter(Reservation::isPaid)
                .sorted(Comparator.comparing(Reservation::getCheckIn)
                        .thenComparing(Reservation::getId))
                .toList();

        BigDecimal totalTax = BigDecimal.ZERO;

        ReportBuilder builder = new ReportBuilder();
        builder.addTitleLine(POUSADA_NAME, "tourism-tax", month.toString());
        builder.addHeaderLine("reservation", "room", "check_in", "check_out", "guests", "nights", "tax");

        for (Reservation reservation : reservations) {
            BigDecimal tax = pricingService.calculateTourismTax(reservation.getGuests(), reservation.getNights());
            totalTax = totalTax.add(tax);

            builder.addLine(
                    String.valueOf(reservation.getId()),
                    reservation.getRoomNumber(),
                    reservation.getCheckIn().format(DATE_FORMATTER),
                    reservation.getCheckOut().format(DATE_FORMATTER),
                    String.valueOf(reservation.getGuests()),
                    String.valueOf(reservation.getNights()),
                    formatDecimal(tax)
            );
        }

        builder.addTotalLine("total", formatDecimal(totalTax));

        return builder.build();
    }

    public String generateRatePlansReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findReservationsStartingInMonth(month)
                .stream()
                .filter(r -> r.getNetAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        Map<RatePlan, List<Reservation>> byPlan = reservations.stream()
                .collect(Collectors.groupingBy(Reservation::getRatePlan));

        ReportBuilder builder = new ReportBuilder();
        builder.addTitleLine(POUSADA_NAME, "rate-plans", month.toString());
        builder.addHeaderLine("rate_plan", "reservations", "amount");

        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> planReservations = byPlan.getOrDefault(plan, Collections.emptyList());
            int count = planReservations.size();
            BigDecimal amount = planReservations.stream()
                    .map(Reservation::getNetAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            builder.addLine(
                    plan.toString(),
                    String.valueOf(count),
                    formatDecimal(amount)
            );

            totalCount += count;
            totalAmount = totalAmount.add(amount);
        }

        builder.addTotalLine("total", String.valueOf(totalCount), formatDecimal(totalAmount));

        return builder.build();
    }

    private String formatDecimal(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }

    private static class ReportBuilder {
        private final StringBuilder sb = new StringBuilder();

        public void addTitleLine(String... parts) {
            sb.append(String.join(",", parts)).append("\n");
        }

        public void addHeaderLine(String... headers) {
            sb.append(String.join(",", headers)).append("\n");
        }

        public void addLine(String... values) {
            sb.append(String.join(",", values)).append("\n");
        }

        public void addTotalLine(String... values) {
            sb.append(String.join(",", values));
        }

        public String build() {
            return sb.toString();
        }
    }
}

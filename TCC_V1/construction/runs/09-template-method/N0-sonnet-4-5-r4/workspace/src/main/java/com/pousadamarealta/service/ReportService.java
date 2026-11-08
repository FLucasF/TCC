package com.pousadamarealta.service;

import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportService {
    private static final BigDecimal TOURISM_TAX_PER_GUEST_NIGHT = new BigDecimal("4.50");
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
        List<Room> allRooms = roomRepository.findAll();
        Map<String, Reservation> occupiedRooms = new HashMap<>();

        for (Room room : allRooms) {
            List<Reservation> reservations = reservationRepository.findByRoomAndDate(room.getNumber(), date);
            if (!reservations.isEmpty()) {
                occupiedRooms.put(room.getNumber(), reservations.get(0));
            }
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,occupancy,").append(DATE_FORMATTER.format(date)).append("\n");
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Room> sortedRooms = allRooms.stream()
            .sorted(Comparator.comparing(Room::getNumber))
            .toList();

        for (Room room : sortedRooms) {
            Reservation reservation = occupiedRooms.get(room.getNumber());
            if (reservation != null) {
                csv.append(room.getNumber()).append(",");
                csv.append(room.getType().name()).append(",");
                csv.append(reservation.getId()).append(",");
                csv.append(DATE_FORMATTER.format(reservation.getCheckIn())).append(",");
                csv.append(DATE_FORMATTER.format(reservation.getCheckOut())).append("\n");
            }
        }

        csv.append("total,").append(occupiedRooms.size()).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(String month) {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);

        List<Reservation> relevantReservations = reservations.stream()
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                        r.getStatus() == ReservationStatus.CHECKED_IN ||
                        r.getStatus() == ReservationStatus.CHECKED_OUT ||
                        (r.getStatus() == ReservationStatus.CANCELLED &&
                         r.getRatePlan() == RatePlan.NON_REFUNDABLE))
            .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,revenue,").append(month).append("\n");
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Reservation reservation : relevantReservations) {
            BigDecimal netAmount = reservation.getAmountPaid();
            if (reservation.getRefund() != null) {
                netAmount = netAmount.subtract(reservation.getRefund());
            }

            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(DATE_FORMATTER.format(reservation.getCheckIn())).append(",");
            csv.append(DATE_FORMATTER.format(reservation.getCheckOut())).append(",");
            csv.append(reservation.getStatus().name()).append(",");
            csv.append(formatCsvNumber(netAmount)).append("\n");

            totalAmount = totalAmount.add(netAmount);
        }

        csv.append("total,").append(formatCsvNumber(totalAmount));

        return csv.toString();
    }

    public String generateTourismTaxReport(String month) {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);

        List<Reservation> paidReservations = reservations.stream()
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                        r.getStatus() == ReservationStatus.CHECKED_IN ||
                        r.getStatus() == ReservationStatus.CHECKED_OUT)
            .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,tourism-tax,").append(month).append("\n");
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        BigDecimal totalTax = BigDecimal.ZERO;

        for (Reservation reservation : paidReservations) {
            BigDecimal tax = TOURISM_TAX_PER_GUEST_NIGHT
                .multiply(BigDecimal.valueOf(reservation.getGuests()))
                .multiply(BigDecimal.valueOf(reservation.getNights()));
            tax = pricingService.roundBankers(tax);

            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getRoomNumber()).append(",");
            csv.append(DATE_FORMATTER.format(reservation.getCheckIn())).append(",");
            csv.append(DATE_FORMATTER.format(reservation.getCheckOut())).append(",");
            csv.append(reservation.getGuests()).append(",");
            csv.append(reservation.getNights()).append(",");
            csv.append(formatCsvNumber(tax)).append("\n");

            totalTax = totalTax.add(tax);
        }

        csv.append("total,").append(formatCsvNumber(totalTax));

        return csv.toString();
    }

    public String generateRatePlansReport(String month) {
        List<Reservation> reservations = reservationRepository.findByCheckInMonth(month);

        List<Reservation> relevantReservations = reservations.stream()
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                        r.getStatus() == ReservationStatus.CHECKED_IN ||
                        r.getStatus() == ReservationStatus.CHECKED_OUT ||
                        (r.getStatus() == ReservationStatus.CANCELLED &&
                         r.getRatePlan() == RatePlan.NON_REFUNDABLE))
            .toList();

        Map<RatePlan, List<Reservation>> byPlan = relevantReservations.stream()
            .collect(Collectors.groupingBy(Reservation::getRatePlan));

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(month).append("\n");
        csv.append("rate_plan,reservations,amount\n");

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalCount = 0;

        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> planReservations = byPlan.getOrDefault(plan, Collections.emptyList());
            int count = planReservations.size();

            BigDecimal amount = planReservations.stream()
                .map(r -> {
                    BigDecimal net = r.getAmountPaid();
                    if (r.getRefund() != null) {
                        net = net.subtract(r.getRefund());
                    }
                    return net;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            csv.append(plan.name()).append(",");
            csv.append(count).append(",");
            csv.append(formatCsvNumber(amount)).append("\n");

            totalAmount = totalAmount.add(amount);
            totalCount += count;
        }

        csv.append("total,").append(totalCount).append(",").append(formatCsvNumber(totalAmount));

        return csv.toString();
    }

    private String formatCsvNumber(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_EVEN).toPlainString();
    }
}

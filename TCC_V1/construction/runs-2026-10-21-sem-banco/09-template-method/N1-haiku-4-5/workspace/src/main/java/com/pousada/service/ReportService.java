package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.domain.Room;
import com.pousada.util.PriceCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class ReportService {
    private final ReservationRepository reservationRepo;
    private final RoomRepository roomRepo;

    public ReportService(ReservationRepository reservationRepo, RoomRepository roomRepo) {
        this.reservationRepo = reservationRepo;
        this.roomRepo = roomRepo;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> occupiedReservations = new ArrayList<>();
        for (Reservation r : reservationRepo.getAllReservations()) {
            if (isPaidAndOccupiedOn(r, date)) {
                occupiedReservations.add(r);
            }
        }
        occupiedReservations.sort(Comparator.comparingInt(Reservation::getRoomNumber));

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,occupancy,").append(formatDateForReport(date)).append("\n");
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : occupiedReservations) {
            Room room = roomRepo.getRoom(r.getRoomNumber());
            csv.append(room.getNumber()).append(",")
                    .append(room.getType().getDisplayName()).append(",")
                    .append(r.getId()).append(",")
                    .append(formatDateForReport(r.getCheckIn())).append(",")
                    .append(formatDateForReport(r.getCheckOut())).append("\n");
        }

        csv.append("total,").append(occupiedReservations.size()).append("/").append(roomRepo.getAllRooms().size()).append("\n");

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> monthReservations = new ArrayList<>();
        for (Reservation r : reservationRepo.getAllReservations()) {
            if (isPaidAndStartsInMonth(r, month)) {
                monthReservations.add(r);
            }
        }
        monthReservations.sort(Comparator.comparingInt((Reservation r) -> r.getCheckIn().getDayOfMonth())
                .thenComparingLong(Reservation::getId));

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,revenue,").append(formatYearMonth(month)).append("\n");
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation r : monthReservations) {
            BigDecimal amount = calculateAmount(r);
            csv.append(r.getId()).append(",")
                    .append(r.getRoomNumber()).append(",")
                    .append(formatDateForReport(r.getCheckIn())).append(",")
                    .append(formatDateForReport(r.getCheckOut())).append(",")
                    .append(r.getStatus()).append(",")
                    .append(formatCurrencyForReport(amount)).append("\n");
            total = total.add(amount);
        }

        csv.append("total,,,,").append(formatCurrencyForReport(total)).append("\n");

        return csv.toString();
    }

    public String generateTourismTaxReport(YearMonth month) {
        List<Reservation> monthReservations = new ArrayList<>();
        for (Reservation r : reservationRepo.getAllReservations()) {
            if (isPaidAndStartsInMonth(r, month)) {
                monthReservations.add(r);
            }
        }
        monthReservations.sort(Comparator.comparingInt((Reservation r) -> r.getCheckIn().getDayOfMonth())
                .thenComparingLong(Reservation::getId));

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,tourism-tax,").append(formatYearMonth(month)).append("\n");
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

        for (Reservation r : monthReservations) {
            BigDecimal tax = PriceCalculator.round(
                    new BigDecimal(r.getGuests() * r.getNights()).multiply(TAX_PER_GUEST_PER_NIGHT)
            );
            csv.append(r.getId()).append(",")
                    .append(r.getRoomNumber()).append(",")
                    .append(formatDateForReport(r.getCheckIn())).append(",")
                    .append(formatDateForReport(r.getCheckOut())).append(",")
                    .append(r.getGuests()).append(",")
                    .append(r.getNights()).append(",")
                    .append(formatCurrencyForReport(tax)).append("\n");
            totalTax = totalTax.add(tax);
        }

        csv.append("total,,,,,").append(formatCurrencyForReport(totalTax)).append("\n");

        return csv.toString();
    }

    public String generateRatePlansReport(YearMonth month) {
        Map<String, List<Reservation>> reservationsByPlan = new HashMap<>();
        reservationsByPlan.put("STANDARD", new ArrayList<>());
        reservationsByPlan.put("NON_REFUNDABLE", new ArrayList<>());

        for (Reservation r : reservationRepo.getAllReservations()) {
            if (isPaidAndStartsInMonth(r, month)) {
                reservationsByPlan.get(r.getRatePlan().name()).add(r);
            }
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(formatYearMonth(month)).append("\n");
        csv.append("rate_plan,reservations,amount\n");

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalReservations = 0;

        for (String planName : Arrays.asList("STANDARD", "NON_REFUNDABLE")) {
            List<Reservation> reservations = reservationsByPlan.get(planName);
            int count = reservations.size();
            BigDecimal amount = BigDecimal.ZERO;

            for (Reservation r : reservations) {
                amount = amount.add(calculateAmount(r));
            }

            csv.append(planName).append(",")
                    .append(count).append(",")
                    .append(formatCurrencyForReport(amount)).append("\n");

            totalAmount = totalAmount.add(amount);
            totalReservations += count;
        }

        csv.append("total,").append(totalReservations).append(",").append(formatCurrencyForReport(totalAmount)).append("\n");

        return csv.toString();
    }

    private boolean isPaidAndOccupiedOn(Reservation r, LocalDate date) {
        if (!isPaid(r)) return false;
        return !date.isBefore(r.getCheckIn()) && date.isBefore(r.getCheckOut());
    }

    private boolean isPaidAndStartsInMonth(Reservation r, YearMonth month) {
        if (!isPaid(r)) return false;
        YearMonth checkInMonth = YearMonth.from(r.getCheckIn());
        return checkInMonth.equals(month);
    }

    private boolean isPaid(Reservation r) {
        ReservationStatus status = r.getStatus();
        return status == ReservationStatus.CONFIRMED || status == ReservationStatus.CHECKED_IN || status == ReservationStatus.CHECKED_OUT;
    }

    private BigDecimal calculateAmount(Reservation r) {
        BigDecimal paid = r.getAmountPaid();
        BigDecimal refunded = r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refunded);
    }

    private String formatDateForReport(LocalDate date) {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private String formatYearMonth(YearMonth month) {
        return String.format("%04d-%02d", month.getYear(), month.getMonthValue());
    }

    private String formatCurrencyForReport(BigDecimal value) {
        return String.format("%.2f", value);
    }
}

package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportService {
    private static final String POUSADA_NAME = "Pousada Maré Alta";
    private static final BigDecimal TOURISM_TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findPaidByNight(date);
        int totalRooms = roomRepository.findAll().size();

        StringBuilder csv = new StringBuilder();
        csv.append(formatTitle("occupancy", date.toString())).append("\n");
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation reservation : reservations) {
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            csv.append(reservation.getRoomNumber()).append(",")
               .append(room.getType()).append(",")
               .append(reservation.getId()).append(",")
               .append(reservation.getCheckIn()).append(",")
               .append(reservation.getCheckOut()).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(totalRooms);

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append(formatTitle("revenue", month.toString())).append("\n");
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            BigDecimal netAmount = calculateNetAmount(reservation);

            if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(reservation.getId()).append(",")
                   .append(reservation.getRoomNumber()).append(",")
                   .append(reservation.getCheckIn()).append(",")
                   .append(reservation.getCheckOut()).append(",")
                   .append(reservation.getStatus()).append(",")
                   .append(formatCsvMoney(netAmount)).append("\n");

                total = total.add(netAmount);
            }
        }

        csv.append("total,").append(formatCsvMoney(total));

        return csv.toString();
    }

    public String generateTourismTaxReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append(formatTitle("tourism-tax", month.toString())).append("\n");
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : reservations) {
            if (isPaidReservation(reservation)) {
                BigDecimal tax = calculateTourismTax(reservation);

                csv.append(reservation.getId()).append(",")
                   .append(reservation.getRoomNumber()).append(",")
                   .append(reservation.getCheckIn()).append(",")
                   .append(reservation.getCheckOut()).append(",")
                   .append(reservation.getGuests()).append(",")
                   .append(reservation.getNights()).append(",")
                   .append(formatCsvMoney(tax)).append("\n");

                total = total.add(tax);
            }
        }

        csv.append("total,").append(formatCsvMoney(total));

        return csv.toString();
    }

    public String generateRatePlansReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByMonth(month);

        Map<RatePlan, List<Reservation>> byRatePlan = reservations.stream()
            .filter(r -> calculateNetAmount(r).compareTo(BigDecimal.ZERO) > 0)
            .collect(Collectors.groupingBy(Reservation::getRatePlan));

        StringBuilder csv = new StringBuilder();
        csv.append(formatTitle("rate-plans", month.toString())).append("\n");
        csv.append("rate_plan,reservations,amount\n");

        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (RatePlan ratePlan : RatePlan.values()) {
            List<Reservation> planReservations = byRatePlan.getOrDefault(ratePlan, List.of());
            int count = planReservations.size();
            BigDecimal amount = planReservations.stream()
                .map(this::calculateNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            csv.append(ratePlan).append(",")
               .append(count).append(",")
               .append(formatCsvMoney(amount)).append("\n");

            totalCount += count;
            totalAmount = totalAmount.add(amount);
        }

        csv.append("total,").append(totalCount).append(",").append(formatCsvMoney(totalAmount));

        return csv.toString();
    }

    private BigDecimal calculateNetAmount(Reservation reservation) {
        BigDecimal paid = reservation.getAmountPaid();
        BigDecimal refund = reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refund);
    }

    private boolean isPaidReservation(Reservation reservation) {
        return reservation.getStatus() == ReservationStatus.CONFIRMED
            || reservation.getStatus() == ReservationStatus.CHECKED_IN
            || reservation.getStatus() == ReservationStatus.CHECKED_OUT;
    }

    private BigDecimal calculateTourismTax(Reservation reservation) {
        int guestNights = reservation.getGuests() * reservation.getNights();
        return TOURISM_TAX_PER_GUEST_PER_NIGHT.multiply(new BigDecimal(guestNights))
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    private String formatTitle(String reportName, String period) {
        return POUSADA_NAME + "," + reportName + "," + period;
    }

    private String formatCsvMoney(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_EVEN).toString();
    }
}

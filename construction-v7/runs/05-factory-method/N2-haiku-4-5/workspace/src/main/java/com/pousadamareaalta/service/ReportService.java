package com.pousadamareaalta.service;

import com.pousadamareaalta.model.Reservation;
import com.pousadamareaalta.model.ReservationStatus;
import com.pousadamareaalta.util.DateUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder sb = new StringBuilder();
        sb.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> occupiedReservations = reservationRepository.findAll().stream()
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
            .filter(r -> r.getStatus() != ReservationStatus.PENDING)
            .filter(r -> isOccupiedOnDate(r, date))
            .sorted(Comparator.comparingInt(Reservation::getRoomNumber))
            .collect(Collectors.toList());

        for (Reservation r : occupiedReservations) {
            String checkInStr = DateUtils.formatDateIso(r.getCheckIn());
            String checkOutStr = DateUtils.formatDateIso(r.getCheckOut());
            sb.append(r.getRoomNumber()).append(",")
                .append(roomRepository.findById(r.getRoomNumber()).type()).append(",")
                .append(r.getId()).append(",")
                .append(checkInStr).append(",")
                .append(checkOutStr).append("\n");
        }

        int occupied = occupiedReservations.size();
        int total = (int) roomRepository.findAll().stream().count();
        sb.append("total,").append(occupied).append("/").append(total);

        return sb.toString();
    }

    public String generateRevenueReport(String month) throws ReservationService.ValidationException {
        YearMonth yearMonth;
        try {
            yearMonth = DateUtils.parseYearMonth(month);
        } catch (Exception e) {
            throw new ReservationService.ValidationException("INVALID_REQUEST", "Formato de mês inválido");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> relevantReservations = reservationRepository.findAll().stream()
            .filter(r -> YearMonth.from(r.getCheckIn()).equals(yearMonth))
            .filter(r -> r.getStatus() != ReservationStatus.PENDING)
            .filter(r -> r.getAmountPaid().compareTo(BigDecimal.ZERO) > 0 || r.getStatus() == ReservationStatus.CANCELLED)
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                .thenComparingLong(Reservation::getId))
            .collect(Collectors.toList());

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Reservation r : relevantReservations) {
            String checkInStr = DateUtils.formatDateIso(r.getCheckIn());
            String checkOutStr = DateUtils.formatDateIso(r.getCheckOut());

            BigDecimal amount = r.getAmountPaid().subtract(
                r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO
            );

            sb.append(r.getId()).append(",")
                .append(r.getRoomNumber()).append(",")
                .append(checkInStr).append(",")
                .append(checkOutStr).append(",")
                .append(r.getStatus()).append(",")
                .append(formatMoneyForCsv(amount)).append("\n");

            totalAmount = totalAmount.add(amount);
        }

        sb.append("total,,,,").append(formatMoneyForCsv(totalAmount));

        return sb.toString();
    }

    private boolean isOccupiedOnDate(Reservation r, LocalDate date) {
        return !date.isBefore(r.getCheckIn()) && date.isBefore(r.getCheckOut());
    }

    private String formatMoneyForCsv(BigDecimal value) {
        return String.format("%.2f", value);
    }
}

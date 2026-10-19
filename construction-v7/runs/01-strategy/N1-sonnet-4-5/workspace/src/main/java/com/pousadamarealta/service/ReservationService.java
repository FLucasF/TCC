package com.pousadamarealta.service;

import com.pousadamarealta.model.*;
import com.pousadamarealta.rateplan.PricingResult;
import com.pousadamarealta.rateplan.RatePlan;
import com.pousadamarealta.rateplan.RatePlanFactory;
import com.pousadamarealta.util.CpfFormatter;
import com.pousadamarealta.util.DateUtils;
import com.pousadamarealta.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationService {
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final RoomService roomService;
    private final MessageService messageService;

    public ReservationService(RoomService roomService, MessageService messageService) {
        this.roomService = roomService;
        this.messageService = messageService;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlanType ratePlanType, String companyCode) {
        Room room = roomService.getRoomByNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException());

        if (guests < 1) {
            throw new InvalidRequestException("Number of guests must be at least 1");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            throw new InvalidRequestException("Check-out date must be after check-in date");
        }

        RatePlan ratePlan = RatePlanFactory.getRatePlan(ratePlanType);

        try {
            ratePlan.validate(room, checkIn, checkOut, companyCode);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut, null)) {
            throw new RoomUnavailableException();
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        PricingResult pricing = ratePlan.calculatePrice(room, checkIn, checkOut, companyCode);

        Reservation reservation = new Reservation(
            nextId.getAndIncrement(),
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            ratePlanType,
            companyCode,
            nights,
            pricing.getSubtotal(),
            pricing.getDiscount(),
            pricing.getTotal()
        );

        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Reservation getReservation(long id) {
        return Optional.ofNullable(reservations.get(id))
                .orElseThrow(() -> new ReservationNotFoundException());
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservation;
    }

    public Reservation checkOut(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        RatePlan ratePlan = RatePlanFactory.getRatePlan(reservation.getRatePlan());
        BigDecimal refund = ratePlan.calculateRefund(reservation.getAmountPaid());

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refund);

        return reservation;
    }

    public String generateDocument(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.PENDING ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new OperationNotAllowedException();
        }

        return buildDocument(reservation);
    }

    public List<Reservation> getReservationsForOccupancyReport(LocalDate date) {
        return reservations.values().stream()
                .filter(r -> r.getStatus() != ReservationStatus.PENDING &&
                           r.getStatus() != ReservationStatus.CANCELLED)
                .filter(r -> !r.getCheckIn().isAfter(date) && r.getCheckOut().isAfter(date))
                .sorted(Comparator.comparingInt(Reservation::getRoomNumber))
                .toList();
    }

    public List<Reservation> getReservationsForRevenueReport(int year, int month) {
        return reservations.values().stream()
                .filter(r -> r.getCheckIn().getYear() == year &&
                           r.getCheckIn().getMonthValue() == month)
                .filter(r -> r.getAmountPaid().compareTo(BigDecimal.ZERO) > 0 ||
                           (r.getRefund() != null && r.getRefund().compareTo(BigDecimal.ZERO) > 0))
                .sorted(Comparator.comparing(Reservation::getCheckIn)
                        .thenComparingLong(Reservation::getId))
                .toList();
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        return reservations.values().stream()
                .filter(r -> excludeReservationId == null || r.getId() != excludeReservationId)
                .filter(r -> r.getRoomNumber() == roomNumber)
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .anyMatch(r -> datesOverlap(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut));
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private void sendConfirmationEmail(Reservation reservation) {
        String document = buildDocument(reservation);
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.getGuestName(),
            document
        );

        messageService.sendEmail(reservation.getGuestEmail(), subject, body);
    }

    private String buildDocument(Reservation reservation) {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber()).orElseThrow();

        StringBuilder doc = new StringBuilder();
        doc.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        doc.append(String.format("Hóspede: %s (CPF %s)\n",
                reservation.getGuestName(),
                CpfFormatter.format(reservation.getGuestCpf())));
        doc.append(String.format("Quarto: %d (%s)\n",
                reservation.getRoomNumber(),
                room.getType()));
        doc.append(String.format("Entrada: %s | Saída: %s | %d %s\n",
                DateUtils.formatForDisplay(reservation.getCheckIn()),
                DateUtils.formatForDisplay(reservation.getCheckOut()),
                reservation.getNights(),
                reservation.getNights() == 1 ? "diária" : "diárias"));
        doc.append(String.format("Diárias: %s\n",
                MoneyUtils.formatMoney(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            doc.append(String.format("Desconto: %s\n",
                    MoneyUtils.formatMoney(reservation.getDiscount())));
        }

        doc.append(String.format("Total: %s",
                MoneyUtils.formatMoney(reservation.getTotal())));

        return doc.toString();
    }

    public static class RoomNotFoundException extends RuntimeException {}
    public static class ReservationNotFoundException extends RuntimeException {}
    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }
    public static class CapacityExceededException extends RuntimeException {}
    public static class RoomUnavailableException extends RuntimeException {}
    public static class OperationNotAllowedException extends RuntimeException {}
}

package com.pousadamareal.service;

import com.pousadamareal.domain.*;
import com.pousadamareal.util.PriceCalculator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class ReservationService {
    private final DataStore dataStore;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ReservationService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public Result<Reservation> createReservation(ReservationRequest request) {
        Room room = dataStore.getRoom(request.roomNumber());
        if (room == null) {
            return Result.error("ROOM_NOT_FOUND", "Room not found");
        }

        if (request.guests() < 1) {
            return Result.error("INVALID_REQUEST", "At least 1 guest is required");
        }

        if (request.guests() > room.capacity) {
            return Result.error("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity");
        }

        if (request.checkOut().isBefore(request.checkIn()) || request.checkOut().isEqual(request.checkIn())) {
            return Result.error("INVALID_REQUEST", "Check-out must be after check-in");
        }

        if (!RatePlanFactory.isValidPlanName(request.ratePlan())) {
            return Result.error("INVALID_REQUEST", "Invalid rate plan");
        }

        RatePlan ratePlan = RatePlanFactory.getPlan(request.ratePlan());
        int nightCount = (int) ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());

        RatePlan.ValidationResult validationResult = ratePlan.validate(request, room, nightCount);
        if (!validationResult.isValid()) {
            return Result.error(validationResult.errorCode().orElse("INVALID_REQUEST"),
                    validationResult.message().orElse("Invalid request"));
        }

        if (isRoomUnavailable(request.roomNumber(), request.checkIn(), request.checkOut())) {
            return Result.error("ROOM_UNAVAILABLE", "Room is not available for the selected dates");
        }

        RatePlan.PricingResult pricingResult = ratePlan.calculatePrice(request, room, nightCount);

        Reservation reservation = new Reservation(
                0,
                request.roomNumber(),
                request.guestName(),
                request.guestEmail(),
                request.guestCpf(),
                request.checkIn(),
                request.checkOut(),
                request.guests(),
                request.ratePlan(),
                nightCount,
                pricingResult.subtotalCents(),
                pricingResult.discountCents(),
                pricingResult.totalCents()
        );

        long reservationId = dataStore.createReservation(reservation);
        Reservation created = dataStore.getReservation(reservationId);
        return Result.ok(created);
    }

    public Result<Reservation> confirmReservation(long reservationId, long amountPaidCents) {
        Reservation reservation = dataStore.getReservation(reservationId);
        if (reservation == null) {
            return Result.error("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.status != Reservation.ReservationStatus.PENDING) {
            return Result.error("OPERATION_NOT_ALLOWED", "Only pending reservations can be confirmed");
        }

        reservation.status = Reservation.ReservationStatus.CONFIRMED;
        reservation.amountPaidCents = amountPaidCents;
        dataStore.updateReservation(reservationId, reservation);

        sendConfirmationEmail(reservation);

        Reservation updated = dataStore.getReservation(reservationId);
        return Result.ok(updated);
    }

    public Result<Reservation> cancelReservation(long reservationId) {
        Reservation reservation = dataStore.getReservation(reservationId);
        if (reservation == null) {
            return Result.error("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.status != Reservation.ReservationStatus.PENDING &&
                reservation.status != Reservation.ReservationStatus.CONFIRMED) {
            return Result.error("OPERATION_NOT_ALLOWED", "Only pending or confirmed reservations can be cancelled");
        }

        RatePlan ratePlan = RatePlanFactory.getPlan(reservation.ratePlan);
        RatePlan.RefundResult refundResult = ratePlan.calculateRefund(reservation.amountPaidCents);

        reservation.status = Reservation.ReservationStatus.CANCELLED;
        reservation.refundCents = refundResult.amountCents();
        dataStore.updateReservation(reservationId, reservation);

        Reservation updated = dataStore.getReservation(reservationId);
        return Result.ok(updated);
    }

    public Result<Reservation> checkIn(long reservationId) {
        Reservation reservation = dataStore.getReservation(reservationId);
        if (reservation == null) {
            return Result.error("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.status != Reservation.ReservationStatus.CONFIRMED) {
            return Result.error("OPERATION_NOT_ALLOWED", "Only confirmed reservations can be checked in");
        }

        reservation.status = Reservation.ReservationStatus.CHECKED_IN;
        dataStore.updateReservation(reservationId, reservation);

        Reservation updated = dataStore.getReservation(reservationId);
        return Result.ok(updated);
    }

    public Result<ReservationReceipt> checkOut(long reservationId) {
        Reservation reservation = dataStore.getReservation(reservationId);
        if (reservation == null) {
            return Result.error("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.status != Reservation.ReservationStatus.CHECKED_IN) {
            return Result.error("OPERATION_NOT_ALLOWED", "Only checked-in reservations can be checked out");
        }

        reservation.status = Reservation.ReservationStatus.CHECKED_OUT;
        dataStore.updateReservation(reservationId, reservation);

        ReservationReceipt receipt = generateReceipt(reservation);
        return Result.ok(receipt);
    }

    public Result<ReservationReceipt> getReceipt(long reservationId) {
        Reservation reservation = dataStore.getReservation(reservationId);
        if (reservation == null) {
            return Result.error("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.amountPaidCents == 0) {
            return Result.error("OPERATION_NOT_ALLOWED", "Second receipt is only available for paid reservations");
        }

        ReservationReceipt receipt = generateReceipt(reservation);
        return Result.ok(receipt);
    }

    private ReservationReceipt generateReceipt(Reservation reservation) {
        Room room = dataStore.getRoom(reservation.roomNumber);
        String roomType = room.type.name();
        String nightsLabel = reservation.nights == 1 ? "1 diária" : reservation.nights + " diárias";

        return new ReservationReceipt(
                "Comprovante de reserva",
                reservation.id,
                reservation.guestName,
                reservation.guestCpf,
                reservation.roomNumber,
                roomType,
                reservation.checkIn.format(DATE_FORMAT),
                reservation.checkOut.format(DATE_FORMAT),
                nightsLabel,
                reservation.subtotalCents,
                reservation.discountCents,
                reservation.totalCents
        );
    }

    private void sendConfirmationEmail(Reservation reservation) {
        ReservationReceipt receipt = generateReceipt(reservation);
        String receiptText = formatReceipt(receipt);

        String emailBody = "Olá, " + reservation.guestName + "!\n\n" +
                "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                receiptText +
                "\n\nA entrada é a partir das 14h, e a saída, até as 12h.\n" +
                "Até breve!";

        dataStore.createMessage("EMAIL", reservation.guestEmail,
                "Reserva confirmada - Pousada Maré Alta", emailBody);
    }

    private String formatReceipt(ReservationReceipt receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(receipt.reservationId()).append("\n");
        sb.append("Hóspede: ").append(receipt.guestName()).append(" (CPF ").append(receipt.guestCpf()).append(")\n");
        sb.append("Quarto: ").append(receipt.roomNumber()).append(" (").append(receipt.roomType()).append(")\n");
        sb.append("Entrada: ").append(receipt.checkIn()).append(" | Saída: ").append(receipt.checkOut())
                .append(" | ").append(receipt.nights()).append("\n");
        sb.append("Diárias: ").append(formatCurrency(receipt.subtotal())).append("\n");

        if (receipt.discount() > 0) {
            sb.append("Desconto: ").append(formatCurrency(receipt.discount())).append("\n");
        }

        sb.append("Total: ").append(formatCurrency(receipt.total())).append("\n");

        return sb.toString();
    }

    private String formatCurrency(long cents) {
        long reais = cents / 100;
        long centavos = cents % 100;
        return String.format("R$ %d.%02d", reais, centavos);
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return dataStore.getAllReservations().stream()
                .filter(r -> r.roomNumber == roomNumber)
                .filter(r -> r.status != Reservation.ReservationStatus.CANCELLED)
                .anyMatch(r -> dateRangesOverlap(r.checkIn, r.checkOut, checkIn, checkOut));
    }

    private boolean dateRangesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    public record Result<T>(boolean success, T data, String errorCode, String errorMessage) {
        public static <T> Result<T> ok(T data) {
            return new Result<>(true, data, null, null);
        }

        public static <T> Result<T> error(String code, String message) {
            return new Result<>(false, null, code, message);
        }
    }

    public record ReservationReceipt(
            String title,
            long reservationId,
            String guestName,
            String guestCpf,
            int roomNumber,
            String roomType,
            String checkIn,
            String checkOut,
            String nights,
            long subtotal,
            long discount,
            long total
    ) {}
}

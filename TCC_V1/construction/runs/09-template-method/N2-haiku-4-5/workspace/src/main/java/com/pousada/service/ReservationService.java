package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.RatePlan;
import com.pousada.domain.ReservationStatus;
import com.pousada.domain.Room;
import com.pousada.repo.ReservationRepository;
import com.pousada.repo.RoomRepository;
import com.pousada.repo.MessageRepository;
import com.pousada.util.DateUtil;
import com.pousada.util.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepo;
    private final RoomRepository roomRepo;
    private final MessageRepository messageRepo;

    public ReservationService(ReservationRepository reservationRepo, RoomRepository roomRepo, MessageRepository messageRepo) {
        this.reservationRepo = reservationRepo;
        this.roomRepo = roomRepo;
        this.messageRepo = messageRepo;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                         LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) throws Exception {
        Room room = roomRepo.findByNumber(roomNumber);
        if (room == null) {
            throw new ValidationException("ROOM_NOT_FOUND", "Room not found");
        }

        if (guests < 1 || guests > room.getCapacity()) {
            throw new ValidationException("CAPACITY_EXCEEDED", "Guests must be between 1 and " + room.getCapacity());
        }

        if (!checkIn.isBefore(checkOut)) {
            throw new ValidationException("INVALID_REQUEST", "Check-out date must be after check-in date");
        }

        List<Reservation> conflictingReservations = reservationRepo.findByRoomAndDates(roomNumber, checkIn, checkOut, ReservationStatus.CANCELLED.name());
        if (!conflictingReservations.isEmpty()) {
            throw new ValidationException("ROOM_UNAVAILABLE", "Room is not available for these dates");
        }

        int nights = DateUtil.countNights(checkIn, checkOut);
        BigDecimal subtotal = calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = calculateDiscount(subtotal, ratePlan);
        BigDecimal total = Money.subtract(subtotal, discount);

        return reservationRepo.create(roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total);
    }

    public Reservation confirmReservation(int reservationId, BigDecimal amountPaid) throws Exception {
        Reservation reservation = reservationRepo.findById(reservationId);
        if (reservation == null) {
            throw new ValidationException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (!reservation.canBeConfirmed()) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Reservation cannot be confirmed in its current state");
        }

        reservationRepo.confirm(reservationId, amountPaid);
        Reservation updated = reservationRepo.findById(reservationId);

        Room room = roomRepo.findByNumber(reservation.getRoomNumber());
        sendConfirmationEmail(updated, room);

        return updated;
    }

    public Reservation cancelReservation(int reservationId) throws Exception {
        Reservation reservation = reservationRepo.findById(reservationId);
        if (reservation == null) {
            throw new ValidationException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (!reservation.canBeCancelled()) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Reservation cannot be cancelled in its current state");
        }

        BigDecimal refund = calculateRefund(reservation);
        reservationRepo.cancel(reservationId, refund);

        return reservationRepo.findById(reservationId);
    }

    public Reservation checkIn(int reservationId) throws Exception {
        Reservation reservation = reservationRepo.findById(reservationId);
        if (reservation == null) {
            throw new ValidationException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (!reservation.canBeCheckedIn()) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Reservation cannot be checked in in its current state");
        }

        reservationRepo.updateStatus(reservationId, ReservationStatus.CHECKED_IN);
        return reservationRepo.findById(reservationId);
    }

    public Reservation checkOut(int reservationId) throws Exception {
        Reservation reservation = reservationRepo.findById(reservationId);
        if (reservation == null) {
            throw new ValidationException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (!reservation.canBeCheckedOut()) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Reservation cannot be checked out in its current state");
        }

        reservationRepo.updateStatus(reservationId, ReservationStatus.CHECKED_OUT);
        return reservationRepo.findById(reservationId);
    }

    public Reservation getReservation(int reservationId) throws Exception {
        Reservation reservation = reservationRepo.findById(reservationId);
        if (reservation == null) {
            throw new ValidationException("RESERVATION_NOT_FOUND", "Reservation not found");
        }
        return reservation;
    }

    private BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentDate = checkIn;

        while (currentDate.isBefore(checkOut)) {
            BigDecimal nightlyRate = room.getNightlyRate();
            if (DateUtil.isHighSeason(currentDate)) {
                nightlyRate = Money.multiply(nightlyRate, new BigDecimal("1.20"));
            }
            subtotal = Money.add(subtotal, nightlyRate);
            currentDate = currentDate.plusDays(1);
        }

        return subtotal;
    }

    private BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return Money.percentage(subtotal, 15);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateRefund(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.PENDING) {
            return BigDecimal.ZERO;
        }

        if (reservation.getRatePlan() == RatePlan.NON_REFUNDABLE) {
            return BigDecimal.ZERO;
        }

        return reservation.getAmountPaid();
    }

    private void sendConfirmationEmail(Reservation reservation, Room room) throws Exception {
        String receipt = buildReceipt(reservation, room);
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildEmailBody(reservation, receipt);

        messageRepo.send(com.pousada.domain.MessageChannel.EMAIL, reservation.getGuestEmail(), subject, body);
    }

    public String buildReceipt(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append(" (CPF ").append(reservation.getGuestCpf()).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(room.getType().getDisplayName()).append(")\n");
        sb.append("Entrada: ").append(DateUtil.formatDate(reservation.getCheckIn()));
        sb.append(" | Saída: ").append(DateUtil.formatDate(reservation.getCheckOut()));
        sb.append(" | ").append(reservation.getNights()).append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: R$ ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: R$ ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: R$ ").append(formatMoney(reservation.getTotal())).append("\n");

        return sb.toString();
    }

    private String buildEmailBody(Reservation reservation, String receipt) {
        return "Olá, " + reservation.getGuestName() + "!\n\n" +
               "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
               receipt + "\n" +
               "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
               "Até breve!\n";
    }

    private String formatMoney(BigDecimal value) {
        String[] parts = value.toPlainString().split("\\.");
        String whole = parts[0];
        String cents = parts.length > 1 ? parts[1] : "00";
        if (cents.length() == 1) {
            cents = cents + "0";
        }
        return whole + "," + cents;
    }
}

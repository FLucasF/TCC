package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.rateplan.CorporatePlan;
import com.pousada.rateplan.RatePlan;
import com.pousada.rateplan.RatePlanFactory;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.utils.ComprovantePrinter;
import com.pousada.utils.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationService {
    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
    private final ReservationRepository reservationRepo;
    private final RoomRepository roomRepo;
    private final MessageService messageService;

    public ReservationService(ReservationRepository reservationRepo, RoomRepository roomRepo, MessageService messageService) {
        this.reservationRepo = reservationRepo;
        this.roomRepo = roomRepo;
        this.messageService = messageService;
    }

    public Reservation create(int roomNumber, String guestName, String guestEmail, String guestCpf,
                             LocalDate checkIn, LocalDate checkOut, int guests, String ratePlanName,
                             String companyCode) throws Exception {
        // Validation
        if (guests < 1) {
            throw new ApiException("INVALID_REQUEST", "At least one guest is required");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new ApiException("INVALID_REQUEST", "Check-out must be after check-in");
        }

        Room room = roomRepo.findById(roomNumber);
        if (room == null) {
            throw new ApiException("ROOM_NOT_FOUND", "Room not found");
        }

        if (guests > room.getCapacity()) {
            throw new ApiException("CAPACITY_EXCEEDED", "Guest count exceeds room capacity");
        }

        if (!RatePlanFactory.exists(ratePlanName)) {
            throw new ApiException("INVALID_REQUEST", "Invalid rate plan");
        }

        // Check availability
        if (!reservationRepo.isRoomAvailable(roomNumber, checkIn, checkOut)) {
            throw new ApiException("ROOM_UNAVAILABLE", "Room is not available for these dates");
        }

        // Get rate plan and validate
        RatePlan ratePlan = RatePlanFactory.getPlan(ratePlanName);
        try {
            ratePlan.validate(room, checkIn, checkOut, guests, companyCode);
        } catch (RatePlan.RatePlanException e) {
            throw new ApiException(e.getCode(), e.getMessage());
        }

        // Calculate price
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        RatePlan.PriceCalculation calc = ratePlan.calculatePrice(room, checkIn, checkOut);
        BigDecimal subtotal = calc.subtotal();
        BigDecimal discount = calc.discount();

        // Apply company discount for CORPORATE plan
        if ("CORPORATE".equals(ratePlanName)) {
            CorporatePlan corporatePlan = (CorporatePlan) ratePlan;
            BigDecimal companyDiscount = corporatePlan.getCompanyDiscount(companyCode);
            discount = Money.percentage(subtotal, companyDiscount);
        }

        BigDecimal total = Money.subtract(subtotal, discount);

        // Save reservation
        Reservation reservation = new Reservation(
            0, roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlanName, companyCode,
            nights, subtotal, discount, total,
            Reservation.Status.PENDING, BigDecimal.ZERO, null
        );

        return reservationRepo.save(reservation);
    }

    public Reservation confirm(long reservationId, BigDecimal amountPaid) throws Exception {
        Reservation reservation = reservationRepo.findById(reservationId);
        if (reservation == null) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.getStatus() != Reservation.Status.PENDING) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Only pending reservations can be confirmed");
        }

        reservationRepo.updateConfirmation(reservationId, amountPaid);

        // Send confirmation email
        Room room = roomRepo.findById(reservation.getRoomNumber());
        String comprovante = ComprovantePrinter.generate(reservation, room.getType().name());
        String emailBody = buildConfirmationEmail(reservation.getGuestName(), comprovante);
        messageService.sendEmail(
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            emailBody
        );

        return reservationRepo.findById(reservationId);
    }

    public Reservation cancel(long reservationId) throws Exception {
        Reservation reservation = reservationRepo.findById(reservationId);
        if (reservation == null) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.getStatus() == Reservation.Status.CHECKED_OUT ||
            reservation.getStatus() == Reservation.Status.CANCELLED) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "This reservation cannot be cancelled");
        }

        // Calculate refund
        RatePlan ratePlan = RatePlanFactory.getPlan(reservation.getRatePlan());
        BigDecimal refund = ratePlan.calculateRefund(reservation.getAmountPaid());

        reservationRepo.updateCancellation(reservationId, refund);
        return reservationRepo.findById(reservationId);
    }

    public Reservation checkIn(long reservationId) throws Exception {
        Reservation reservation = reservationRepo.findById(reservationId);
        if (reservation == null) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.getStatus() != Reservation.Status.CONFIRMED) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Only confirmed reservations can check in");
        }

        reservationRepo.updateStatus(reservationId, Reservation.Status.CHECKED_IN);
        return reservationRepo.findById(reservationId);
    }

    public Reservation checkOut(long reservationId) throws Exception {
        Reservation reservation = reservationRepo.findById(reservationId);
        if (reservation == null) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.getStatus() != Reservation.Status.CHECKED_IN) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Only checked-in reservations can check out");
        }

        reservationRepo.updateStatus(reservationId, Reservation.Status.CHECKED_OUT);
        return reservationRepo.findById(reservationId);
    }

    private String buildConfirmationEmail(String guestName, String comprovante) {
        return "Olá, " + guestName + "!\n\n" +
               "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
               comprovante + "\n\n" +
               "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
               "Até breve!";
    }

    public static class ApiException extends Exception {
        private final String code;

        public ApiException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}

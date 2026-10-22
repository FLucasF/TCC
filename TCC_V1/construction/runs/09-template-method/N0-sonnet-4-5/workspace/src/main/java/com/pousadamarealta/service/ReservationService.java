package com.pousadamarealta.service;

import com.pousadamarealta.dto.ReceiptDto;
import com.pousadamarealta.exception.*;
import com.pousadamarealta.model.*;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final PricingService pricingService;
    private final NotificationService notificationService;

    public ReservationService(
            ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            PricingService pricingService,
            NotificationService notificationService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.pricingService = pricingService;
        this.notificationService = notificationService;
    }

    public Reservation createReservation(
            String roomNumber,
            String guestName,
            String guestEmail,
            String guestCpf,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            RatePlan ratePlan) {

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        Room room = roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomNumber));

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        if (reservationRepository.hasConflict(roomNumber, checkIn, checkOut, null)) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setGuestCpf(guestCpf);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(ratePlan);

        calculatePricing(reservation, room);

        return reservationRepository.save(reservation);
    }

    private void calculatePricing(Reservation reservation, Room room) {
        int nights = (int) ChronoUnit.DAYS.between(reservation.getCheckIn(), reservation.getCheckOut());
        reservation.setNights(nights);

        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = reservation.getCheckIn();
        for (int i = 0; i < nights; i++) {
            BigDecimal nightPrice = pricingService.calculateNightPrice(room.getNightlyRate(), current);
            subtotal = subtotal.add(nightPrice);
            current = current.plusDays(1);
        }
        subtotal = pricingService.round(subtotal);
        reservation.setSubtotal(subtotal);

        BigDecimal discount = BigDecimal.ZERO;
        if (reservation.getRatePlan() == RatePlan.NON_REFUNDABLE) {
            discount = pricingService.round(subtotal.multiply(new BigDecimal("0.15")));
        }
        reservation.setDiscount(discount);

        BigDecimal total = pricingService.round(subtotal.subtract(discount));
        reservation.setTotal(total);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Only pending reservations can be confirmed");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.update(reservation);

        ReceiptDto receipt = buildReceipt(reservation);
        notificationService.sendConfirmationEmail(
                reservation.getGuestName(),
                reservation.getGuestEmail(),
                receipt);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Only pending or confirmed reservations can be cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        if (reservation.getRatePlan() == RatePlan.STANDARD &&
            reservation.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            reservation.setRefund(reservation.getAmountPaid());
        } else {
            reservation.setRefund(BigDecimal.ZERO);
        }

        reservationRepository.update(reservation);
        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Only confirmed reservations can be checked in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.update(reservation);
        return reservation;
    }

    public ReceiptDto checkOut(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Only checked-in reservations can be checked out");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.update(reservation);

        return buildReceipt(reservation);
    }

    public ReceiptDto getDocument(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED &&
            reservation.getStatus() != ReservationStatus.CHECKED_IN &&
            reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            throw new OperationNotAllowedException("Document only available for paid reservations");
        }

        return buildReceipt(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));
    }

    private ReceiptDto buildReceipt(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        return new ReceiptDto(
                "Comprovante de reserva",
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestCpf(),
                reservation.getRoomNumber(),
                room.getType().name(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal()
        );
    }
}

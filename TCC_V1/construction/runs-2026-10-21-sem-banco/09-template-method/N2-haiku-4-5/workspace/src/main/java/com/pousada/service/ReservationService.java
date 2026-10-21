package com.pousada.service;

import com.pousada.domain.*;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepo;
    private final RoomRepository roomRepo;
    private final MessageRepository messageRepo;
    private final PricingService pricingService;

    public ReservationService(ReservationRepository reservationRepo, RoomRepository roomRepo,
                             MessageRepository messageRepo, PricingService pricingService) {
        this.reservationRepo = reservationRepo;
        this.roomRepo = roomRepo;
        this.messageRepo = messageRepo;
        this.pricingService = pricingService;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) throws Exception {
        if (checkIn.isAfter(checkOut) || checkIn.isEqual(checkOut)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (guests < 1) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        Optional<Room> room = roomRepo.findById(roomNumber);
        if (room.isEmpty()) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        Room r = room.get();
        if (guests > r.getCapacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        PricingService.PricingResult pricing = pricingService.calculatePrice(r.getNightlyRate(), checkIn, checkOut, ratePlan);

        long reservationId = reservationRepo.getNextId();
        Reservation reservation = new Reservation(
            reservationId,
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            ratePlan,
            pricing.nights,
            pricing.subtotal,
            pricing.discount,
            pricing.total
        );

        return reservationRepo.save(reservation);
    }

    public Reservation confirmReservation(long reservationId) throws Exception {
        Optional<Reservation> opt = reservationRepo.findById(reservationId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        Reservation reservation = opt.get();
        if (!reservation.canBeConfirmed()) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepo.save(reservation);

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation checkIn(long reservationId) throws Exception {
        Optional<Reservation> opt = reservationRepo.findById(reservationId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        Reservation reservation = opt.get();
        if (!reservation.canBeCheckedIn()) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservationRepo.save(reservation);
    }

    public Receipt checkOut(long reservationId) throws Exception {
        Optional<Reservation> opt = reservationRepo.findById(reservationId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        Reservation reservation = opt.get();
        if (!reservation.canBeCheckedOut()) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepo.save(reservation);

        Optional<Room> room = roomRepo.findById(reservation.getRoomNumber());
        Room r = room.get();

        return new Receipt(
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            r.getType(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }

    public Receipt getReceipt(long reservationId) throws Exception {
        Optional<Reservation> opt = reservationRepo.findById(reservationId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        Reservation reservation = opt.get();
        if (!reservation.isPaid()) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        Optional<Room> room = roomRepo.findById(reservation.getRoomNumber());
        Room r = room.get();

        return new Receipt(
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            r.getType(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }

    public Reservation cancelReservation(long reservationId) throws Exception {
        Optional<Reservation> opt = reservationRepo.findById(reservationId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        Reservation reservation = opt.get();
        if (!reservation.canBeCancelled()) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        if (reservation.getRatePlan() == RatePlan.STANDARD) {
            reservation.setRefund(reservation.getAmountPaid());
        } else {
            reservation.setRefund(BigDecimal.ZERO);
        }

        return reservationRepo.save(reservation);
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        LocalDate currentDate = checkIn;
        while (currentDate.isBefore(checkOut)) {
            for (Reservation res : reservationRepo.findAll().values()) {
                if (res.getRoomNumber() == roomNumber && res.getStatus() != ReservationStatus.CANCELLED) {
                    LocalDate resStart = res.getCheckIn();
                    LocalDate resEnd = res.getCheckOut();

                    if (!currentDate.isBefore(resStart) && currentDate.isBefore(resEnd)) {
                        return true;
                    }
                }
            }
            currentDate = currentDate.plusDays(1);
        }
        return false;
    }

    private void sendConfirmationEmail(Reservation reservation) {
        Optional<Room> room = roomRepo.findById(reservation.getRoomNumber());
        Room r = room.get();

        Receipt receipt = new Receipt(
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            r.getType(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );

        String body = "Olá, " + reservation.getGuestName() + "!\n\n" +
                     "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                     receipt.formatAsText() + "\n\n" +
                     "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                     "Até breve!";

        messageRepo.save("EMAIL", reservation.getGuestEmail(), "Reserva confirmada - Pousada Maré Alta", body);
    }
}

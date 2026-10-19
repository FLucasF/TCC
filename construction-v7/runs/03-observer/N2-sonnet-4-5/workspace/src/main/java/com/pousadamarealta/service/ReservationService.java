package com.pousadamarealta.service;

import com.pousadamarealta.exception.ApiException;
import com.pousadamarealta.model.*;
import com.pousadamarealta.observer.ReservationEvent;
import com.pousadamarealta.observer.ReservationEventPublisher;
import com.pousadamarealta.observer.ReservationEventType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationService {
    private final Map<String, Room> rooms = new HashMap<>();
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final PricingService pricingService;
    private final ReservationEventPublisher eventPublisher;

    public ReservationService(PricingService pricingService, ReservationEventPublisher eventPublisher) {
        this.pricingService = pricingService;
        this.eventPublisher = eventPublisher;
        initializeRooms();
    }

    private void initializeRooms() {
        rooms.put("101", new Room("101", RoomType.STANDARD));
        rooms.put("102", new Room("102", RoomType.STANDARD));
        rooms.put("201", new Room("201", RoomType.DELUXE));
        rooms.put("202", new Room("202", RoomType.DELUXE));
        rooms.put("301", new Room("301", RoomType.SUITE));
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public Room getRoom(String number) {
        Room room = rooms.get(number);
        if (room == null) {
            throw new ApiException("ROOM_NOT_FOUND", "Quarto não encontrado", 404);
        }
        return room;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
        Room room = getRoom(roomNumber);

        if (guests < 1) {
            throw new ApiException("INVALID_REQUEST", "Número de hóspedes deve ser pelo menos 1", 400);
        }

        if (guests > room.getCapacity()) {
            throw new ApiException("CAPACITY_EXCEEDED",
                    "Número de hóspedes excede a capacidade do quarto", 422);
        }

        if (!isRoomAvailable(roomNumber, checkIn, checkOut)) {
            throw new ApiException("ROOM_UNAVAILABLE", "Quarto não disponível nas datas solicitadas", 409);
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = pricingService.calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = pricingService.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        long id = nextId.getAndIncrement();
        Reservation reservation = new Reservation(id, roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total);

        reservations.put(id, reservation);
        return reservation;
    }

    public Reservation getReservation(long id) {
        Reservation reservation = reservations.get(id);
        if (reservation == null) {
            throw new ApiException("RESERVATION_NOT_FOUND", "Reserva não encontrada", 404);
        }
        return reservation;
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ApiException("OPERATION_NOT_ALLOWED",
                    "Apenas reservas pendentes podem ser confirmadas", 409);
        }

        Reservation previousState = copyReservation(reservation);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());

        eventPublisher.notify(new ReservationEvent(ReservationEventType.CONFIRMED, reservation, previousState));

        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
            reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new ApiException("OPERATION_NOT_ALLOWED",
                    "Não é possível cancelar uma reserva após o check-in", 409);
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ApiException("OPERATION_NOT_ALLOWED",
                    "Reserva já está cancelada", 409);
        }

        Reservation previousState = copyReservation(reservation);
        reservation.setStatus(ReservationStatus.CANCELLED);

        BigDecimal refundAmount = BigDecimal.ZERO;
        if (reservation.getRatePlan() == RatePlan.STANDARD) {
            refundAmount = reservation.getAmountPaid();
        }
        reservation.setRefund(refundAmount);

        eventPublisher.notify(new ReservationEvent(ReservationEventType.CANCELLED, reservation, previousState));

        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ApiException("OPERATION_NOT_ALLOWED",
                    "Apenas reservas confirmadas podem fazer check-in", 409);
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservation;
    }

    public Reservation checkOut(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new ApiException("OPERATION_NOT_ALLOWED",
                    "Apenas reservas com check-in podem fazer check-out", 409);
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return reservation;
    }

    public List<Reservation> getAllReservations() {
        return new ArrayList<>(reservations.values());
    }

    private boolean isRoomAvailable(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return reservations.values().stream()
                .filter(r -> r.getRoomNumber().equals(roomNumber))
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .noneMatch(r -> datesOverlap(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut));
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private Reservation copyReservation(Reservation reservation) {
        Reservation copy = new Reservation(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            reservation.getGuestCpf(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getGuests(),
            reservation.getRatePlan(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
        copy.setStatus(reservation.getStatus());
        copy.setAmountPaid(reservation.getAmountPaid());
        copy.setRefund(reservation.getRefund());
        return copy;
    }
}

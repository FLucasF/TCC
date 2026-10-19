package com.pousada.services;

import com.pousada.models.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationService {
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private final Map<Integer, Room> rooms = new HashMap<>();
    private final AtomicLong reservationIdCounter = new AtomicLong(1);
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReservationService() {
        initializeRooms();
    }

    private void initializeRooms() {
        rooms.put(101, new Room(101, Room.RoomType.STANDARD, 2, BigDecimal.valueOf(249.90)));
        rooms.put(102, new Room(102, Room.RoomType.STANDARD, 2, BigDecimal.valueOf(249.90)));
        rooms.put(201, new Room(201, Room.RoomType.DELUXE, 3, BigDecimal.valueOf(389.90)));
        rooms.put(202, new Room(202, Room.RoomType.DELUXE, 3, BigDecimal.valueOf(389.90)));
        rooms.put(301, new Room(301, Room.RoomType.SUITE, 4, BigDecimal.valueOf(579.90)));
    }

    public Room getRoom(int roomNumber) {
        return rooms.get(roomNumber);
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                        String guestCpf, String checkInStr, String checkOutStr,
                                        int guests, String ratePlanCode, String companyCode) {
        validateBasicInput(roomNumber, checkInStr, checkOutStr, guests);

        Room room = getRoom(roomNumber);
        if (room == null) {
            throw new ApiException("ROOM_NOT_FOUND", "Quarto não encontrado", 404);
        }

        LocalDate checkIn = LocalDate.parse(checkInStr, dateFormatter);
        LocalDate checkOut = LocalDate.parse(checkOutStr, dateFormatter);

        validateDateRange(checkIn, checkOut);

        RatePlan ratePlan = createRatePlan(ratePlanCode, room.getType());
        try {
            ratePlan.validate(room.getCapacity(), guests, checkIn, checkOut, companyCode);
        } catch (RatePlan.ValidationException e) {
            int statusCode = "CAPACITY_EXCEEDED".equals(e.getCode()) ? 422 : 400;
            throw new ApiException(e.getCode(), e.getMessage(), statusCode);
        }

        checkRoomAvailability(roomNumber, checkIn, checkOut);

        int nights = PricingUtils.countNights(checkIn, checkOut);
        BigDecimal subtotal = ratePlan.calculateSubtotal(room.getNightlyRate(), nights, checkIn, checkOut, room.getType());
        BigDecimal discount = ratePlan.calculateDiscount(subtotal);
        BigDecimal total = PricingUtils.roundHalfEven(subtotal.subtract(discount));

        long id = reservationIdCounter.getAndIncrement();
        Reservation reservation = new Reservation(id, roomNumber, guestName, guestEmail, guestCpf,
                                                  checkIn, checkOut, guests, ratePlan, nights,
                                                  subtotal, discount, total);
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

    public Reservation confirmReservation(long id, BigDecimal amountPaid) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != Reservation.ReservationStatus.PENDING) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Operação não permitida para esta reserva", 409);
        }

        reservation.setAmountPaid(amountPaid);
        reservation.setStatus(Reservation.ReservationStatus.CONFIRMED);
        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != Reservation.ReservationStatus.CONFIRMED) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Operação não permitida para esta reserva", 409);
        }

        reservation.setStatus(Reservation.ReservationStatus.CHECKED_IN);
        return reservation;
    }

    public Reservation checkOut(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != Reservation.ReservationStatus.CHECKED_IN) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Operação não permitida para esta reserva", 409);
        }

        reservation.setStatus(Reservation.ReservationStatus.CHECKED_OUT);
        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != Reservation.ReservationStatus.PENDING &&
            reservation.getStatus() != Reservation.ReservationStatus.CONFIRMED) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Operação não permitida para esta reserva", 409);
        }

        BigDecimal refund = reservation.getRatePlan().calculateRefund(reservation);
        reservation.setRefund(refund);
        reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
        return reservation;
    }

    public boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            for (Reservation reservation : reservations.values()) {
                if (reservation.getRoomNumber() == roomNumber &&
                    reservation.getStatus() != Reservation.ReservationStatus.CANCELLED) {
                    if (!current.isBefore(reservation.getCheckIn()) &&
                        current.isBefore(reservation.getCheckOut())) {
                        return false;
                    }
                }
            }
            current = current.plusDays(1);
        }
        return true;
    }

    public List<Reservation> getReservationsForRoom(int roomNumber, LocalDate date) {
        List<Reservation> result = new ArrayList<>();
        for (Reservation reservation : reservations.values()) {
            if (reservation.getRoomNumber() == roomNumber &&
                reservation.getStatus() != Reservation.ReservationStatus.CANCELLED &&
                (reservation.getStatus() == Reservation.ReservationStatus.CONFIRMED ||
                 reservation.getStatus() == Reservation.ReservationStatus.CHECKED_IN ||
                 reservation.getStatus() == Reservation.ReservationStatus.CHECKED_OUT)) {
                if (!date.isBefore(reservation.getCheckIn()) && date.isBefore(reservation.getCheckOut())) {
                    result.add(reservation);
                }
            }
        }
        return result;
    }

    public List<Reservation> getReservationsForMonth(int year, int month) {
        List<Reservation> result = new ArrayList<>();
        for (Reservation reservation : reservations.values()) {
            if (reservation.getCheckIn().getYear() == year &&
                reservation.getCheckIn().getMonthValue() == month &&
                (reservation.getStatus() == Reservation.ReservationStatus.CONFIRMED ||
                 reservation.getStatus() == Reservation.ReservationStatus.CHECKED_IN ||
                 reservation.getStatus() == Reservation.ReservationStatus.CHECKED_OUT ||
                 reservation.getStatus() == Reservation.ReservationStatus.CANCELLED)) {
                result.add(reservation);
            }
        }
        return result;
    }

    private void validateBasicInput(int roomNumber, String checkInStr, String checkOutStr, int guests) {
        if (guests < 1) {
            throw new ApiException("INVALID_REQUEST", "Número de hóspedes inválido", 400);
        }

        try {
            LocalDate.parse(checkInStr, dateFormatter);
            LocalDate.parse(checkOutStr, dateFormatter);
        } catch (Exception e) {
            throw new ApiException("INVALID_REQUEST", "Formato de data inválido", 400);
        }
    }

    private void validateDateRange(LocalDate checkIn, LocalDate checkOut) {
        if (!checkIn.isBefore(checkOut)) {
            throw new ApiException("INVALID_REQUEST", "Data de saída deve ser depois da entrada", 400);
        }
    }

    private void checkRoomAvailability(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        if (!isRoomAvailable(roomNumber, checkIn, checkOut)) {
            throw new ApiException("ROOM_UNAVAILABLE", "Quarto não disponível nas datas solicitadas", 409);
        }
    }

    private RatePlan createRatePlan(String ratePlanCode, Room.RoomType roomType) {
        return switch (ratePlanCode) {
            case "STANDARD" -> new StandardRatePlan();
            case "NON_REFUNDABLE" -> new NonRefundableRatePlan();
            case "CORPORATE" -> new CorporateRatePlan();
            case "WEEKEND_PACKAGE" -> new WeekendPackageRatePlan();
            case "LONG_STAY" -> new LongStayRatePlan();
            default -> throw new ApiException("INVALID_REQUEST", "Plano de tarifa inválido", 400);
        };
    }

    public static class ApiException extends RuntimeException {
        private final String code;
        private final int statusCode;

        public ApiException(String code, String message, int statusCode) {
            super(message);
            this.code = code;
            this.statusCode = statusCode;
        }

        public String getCode() {
            return code;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}

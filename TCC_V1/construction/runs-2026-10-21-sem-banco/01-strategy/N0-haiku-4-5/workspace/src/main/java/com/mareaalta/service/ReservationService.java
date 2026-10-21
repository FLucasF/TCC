package com.mareaalta.service;

import com.mareaalta.model.Reservation;
import com.mareaalta.model.Room;
import com.mareaalta.data.RoomData;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ReservationService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private Map<Integer, Reservation> reservations = new HashMap<>();
    private int nextId = 1;

    public ReservationService() {}

    public Room getRoomByNumber(int roomNumber) {
        return RoomData.getRooms().stream()
            .filter(r -> r.number() == roomNumber)
            .findFirst()
            .orElse(null);
    }

    public List<Room> getAllRooms() {
        return RoomData.getRooms();
    }

    public String validateCreateReservation(int roomNumber, String checkIn, String checkOut, int guests, String ratePlan, String companyCode) {
        if (!isValidDateFormat(checkIn) || !isValidDateFormat(checkOut)) {
            return "INVALID_REQUEST";
        }

        LocalDate checkInDate = LocalDate.parse(checkIn, DATE_FORMAT);
        LocalDate checkOutDate = LocalDate.parse(checkOut, DATE_FORMAT);

        if (!checkOutDate.isAfter(checkInDate)) {
            return "INVALID_REQUEST";
        }

        if (guests < 1) {
            return "INVALID_REQUEST";
        }

        Room room = getRoomByNumber(roomNumber);
        if (room == null) {
            return "ROOM_NOT_FOUND";
        }

        if (guests > room.capacity()) {
            return "CAPACITY_EXCEEDED";
        }

        if (!isRatePlanValid(ratePlan, checkIn, checkOut, companyCode)) {
            return "INVALID_REQUEST";
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            return "ROOM_UNAVAILABLE";
        }

        return null;
    }

    private boolean isRatePlanValid(String ratePlan, String checkIn, String checkOut, String companyCode) {
        return switch (ratePlan) {
            case "STANDARD", "NON_REFUNDABLE" -> true;
            case "LONG_STAY" -> isValidLongStay(checkIn, checkOut);
            case "CORPORATE" -> companyCode != null && isValidCompanyCode(companyCode);
            case "WEEKEND_PACKAGE" -> isValidWeekendPackage(checkIn, checkOut);
            default -> false;
        };
    }

    private boolean isValidLongStay(String checkIn, String checkOut) {
        try {
            int nights = PricingService.calculateNights(checkIn, checkOut);
            return nights >= 7;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidCompanyCode(String code) {
        return code.equals("TECNOSUL") || code.equals("AGROVALE") || code.equals("FARMAVIDA");
    }

    private boolean isValidWeekendPackage(String checkIn, String checkOut) {
        try {
            LocalDate checkInDate = LocalDate.parse(checkIn, DATE_FORMAT);
            LocalDate checkOutDate = LocalDate.parse(checkOut, DATE_FORMAT);

            int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkInDate, checkOutDate);
            if (nights != 2) return false;

            if (checkInDate.getDayOfWeek() != java.time.DayOfWeek.FRIDAY) return false;
            if (checkOutDate.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) return false;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidDateFormat(String date) {
        try {
            LocalDate.parse(date, DATE_FORMAT);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                        String checkIn, String checkOut, int guests, String ratePlan, String companyCode) {
        int id = nextId++;
        Reservation res = new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan, companyCode);

        int nights = PricingService.calculateNights(checkIn, checkOut);
        res.nights = nights;

        Room room = getRoomByNumber(roomNumber);
        PricingService.PricingResult pricing = PricingService.calculatePrice(ratePlan, nights, room.nightlyRate(), checkIn, checkOut, companyCode);

        if (pricing != null) {
            res.subtotal = pricing.subtotal;
            res.discount = pricing.discount;
            res.total = pricing.total;
        }

        reservations.put(id, res);
        return res;
    }

    public Reservation getReservation(int id) {
        return reservations.get(id);
    }

    private boolean isRoomUnavailable(int roomNumber, String checkIn, String checkOut) {
        LocalDate checkInDate = LocalDate.parse(checkIn, DATE_FORMAT);
        LocalDate checkOutDate = LocalDate.parse(checkOut, DATE_FORMAT);

        for (Reservation res : reservations.values()) {
            if (res.roomNumber == roomNumber && !res.status.equals("CANCELLED")) {
                LocalDate resCheckIn = LocalDate.parse(res.checkIn, DATE_FORMAT);
                LocalDate resCheckOut = LocalDate.parse(res.checkOut, DATE_FORMAT);

                if (checkInDate.isBefore(resCheckOut) && checkOutDate.isAfter(resCheckIn)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String confirmReservation(int id) {
        Reservation res = getReservation(id);
        if (res == null) {
            return "RESERVATION_NOT_FOUND";
        }
        if (!res.status.equals("PENDING")) {
            return "OPERATION_NOT_ALLOWED";
        }
        res.status = "CONFIRMED";
        res.amountPaid = res.total;
        return null;
    }

    public String cancelReservation(int id) {
        Reservation res = getReservation(id);
        if (res == null) {
            return "RESERVATION_NOT_FOUND";
        }
        if (!res.status.equals("PENDING") && !res.status.equals("CONFIRMED")) {
            return "OPERATION_NOT_ALLOWED";
        }

        res.status = "CANCELLED";

        if (res.amountPaid > 0) {
            double refundAmount = calculateRefund(res);
            res.refund = PricingService.roundToTwoCents(refundAmount);
        } else {
            res.refund = 0.0;
        }

        return null;
    }

    private double calculateRefund(Reservation res) {
        return switch (res.ratePlan) {
            case "STANDARD", "CORPORATE", "LONG_STAY" -> res.amountPaid;
            case "NON_REFUNDABLE" -> 0;
            case "WEEKEND_PACKAGE" -> PricingService.roundToTwoCents(res.amountPaid * 0.5);
            default -> 0;
        };
    }

    public String checkIn(int id) {
        Reservation res = getReservation(id);
        if (res == null) {
            return "RESERVATION_NOT_FOUND";
        }
        if (!res.status.equals("CONFIRMED")) {
            return "OPERATION_NOT_ALLOWED";
        }
        res.status = "CHECKED_IN";
        return null;
    }

    public String checkOut(int id) {
        Reservation res = getReservation(id);
        if (res == null) {
            return "RESERVATION_NOT_FOUND";
        }
        if (!res.status.equals("CHECKED_IN")) {
            return "OPERATION_NOT_ALLOWED";
        }
        res.status = "CHECKED_OUT";
        return null;
    }

    public Collection<Reservation> getAllReservations() {
        return reservations.values();
    }
}

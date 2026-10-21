package com.pousada.service;

import com.pousada.domain.RatePlan;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.domain.Room;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ReservationService {
    private final Map<Integer, Reservation> reservations = new TreeMap<>();
    private int nextReservationId = 1;
    private final RoomService roomService;
    private final PricingService pricingService;

    public ReservationService(RoomService roomService, PricingService pricingService) {
        this.roomService = roomService;
        this.pricingService = pricingService;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
            String companyCnpj, String companyName, String guestPassport, String guestCountry, LocalDate checkIn,
            LocalDate checkOut, int guests, RatePlan ratePlan) {

        if (!roomService.roomExists(roomNumber)) {
            throw new IllegalArgumentException("Room not found");
        }

        Room room = roomService.getRoom(roomNumber);

        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }

        if (guests < 1 || guests > room.type.capacity) {
            throw new IllegalArgumentException("Invalid number of guests");
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("Room is unavailable for these dates");
        }

        var pricing = pricingService.calculatePrice(room.type.nightlyRate, checkIn, checkOut, ratePlan);

        Reservation reservation = new Reservation(nextReservationId++, roomNumber, guestName, guestEmail, guestCpf,
                companyCnpj, companyName, guestPassport, guestCountry, checkIn, checkOut, guests, ratePlan,
                pricing.nights(), pricing.subtotal(), pricing.discount(), pricing.total());

        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Reservation getReservation(int id) {
        return reservations.get(id);
    }

    public Reservation confirmReservation(int id, double amountPaid) {
        Reservation res = reservations.get(id);
        if (res == null) {
            throw new IllegalArgumentException("Reservation not found");
        }
        res.confirm(amountPaid);
        return res;
    }

    public Reservation checkInReservation(int id) {
        Reservation res = reservations.get(id);
        if (res == null) {
            throw new IllegalArgumentException("Reservation not found");
        }
        res.checkIn();
        return res;
    }

    public Reservation checkOutReservation(int id) {
        Reservation res = reservations.get(id);
        if (res == null) {
            throw new IllegalArgumentException("Reservation not found");
        }
        res.checkOut();
        return res;
    }

    public Reservation cancelReservation(int id) {
        Reservation res = reservations.get(id);
        if (res == null) {
            throw new IllegalArgumentException("Reservation not found");
        }

        double refund = 0;
        if (res.getStatus() == ReservationStatus.CONFIRMED) {
            if (res.getRatePlan() == RatePlan.STANDARD) {
                refund = res.getAmountPaid();
            }
        }

        res.cancel(refund > 0 ? refund : null);
        return res;
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            final LocalDate checkDate = current;
            boolean occupied = reservations.values().stream()
                    .filter(r -> r.getRoomNumber() == roomNumber && r.getStatus() != ReservationStatus.CANCELLED)
                    .anyMatch(r -> !r.getCheckIn().isAfter(checkDate) && r.getCheckOut().isAfter(checkDate));

            if (occupied) {
                return true;
            }
            current = current.plusDays(1);
        }
        return false;
    }

    public List<Reservation> getReservationsForRoom(int roomNumber, LocalDate date) {
        return reservations.values().stream()
                .filter(r -> r.getRoomNumber() == roomNumber && r.getStatus() != ReservationStatus.CANCELLED
                        && !r.getCheckIn().isAfter(date) && r.getCheckOut().isAfter(date))
                .toList();
    }

    public List<Reservation> getReservationsStartingInMonth(int year, int month) {
        return reservations.values().stream()
                .filter(r -> r.getCheckIn().getYear() == year && r.getCheckIn().getMonthValue() == month
                        && (r.getStatus() == ReservationStatus.CONFIRMED || r.getStatus() == ReservationStatus.CHECKED_IN
                                || r.getStatus() == ReservationStatus.CHECKED_OUT || r.getStatus() == ReservationStatus.CANCELLED)
                        && r.getAmountPaid() > 0)
                .sorted((a, b) -> {
                    int dateCompare = a.getCheckIn().compareTo(b.getCheckIn());
                    if (dateCompare == 0) {
                        return Integer.compare(a.getId(), b.getId());
                    }
                    return dateCompare;
                })
                .toList();
    }
}

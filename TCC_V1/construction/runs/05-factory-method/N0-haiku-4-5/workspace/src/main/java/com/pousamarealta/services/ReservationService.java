package com.pousamarealta.services;

import com.pousamarealta.models.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class ReservationService {
    private Map<Long, Reservation> reservations = new HashMap<>();
    private long nextReservationId = 1;
    private RoomService roomService = new RoomService();

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                         String guestCpf, String companyCnpj, String companyName,
                                         String guestPassport, String guestCountry,
                                         LocalDate checkIn, LocalDate checkOut, int guests,
                                         RatePlan ratePlan) {
        Reservation reservation = new Reservation();
        reservation.setId(nextReservationId++);
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setGuestCpf(guestCpf);
        reservation.setCompanyCnpj(companyCnpj);
        reservation.setCompanyName(companyName);
        reservation.setGuestPassport(guestPassport);
        reservation.setGuestCountry(guestCountry);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(ratePlan);

        Room room = roomService.getRoomByNumber(roomNumber);
        int nights = PriceService.calculateNights(checkIn, checkOut);
        BigDecimal subtotal = PriceService.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        BigDecimal discount = PriceService.calculateDiscount(subtotal, ratePlan.getValue());
        BigDecimal total = PriceService.calculateTotal(subtotal, discount);

        reservation.setNights(nights);
        reservation.setSubtotal(subtotal);
        reservation.setDiscount(discount);
        reservation.setTotal(total);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setAmountPaid(BigDecimal.ZERO);
        reservation.setRefund(null);

        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Reservation getReservation(long id) {
        return reservations.get(id);
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = reservations.get(id);
        if (reservation != null && reservation.getStatus() == ReservationStatus.PENDING) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservation.setAmountPaid(reservation.getTotal());
        }
        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = reservations.get(id);
        if (reservation != null && reservation.getStatus() == ReservationStatus.CONFIRMED) {
            reservation.setStatus(ReservationStatus.CHECKED_IN);
        }
        return reservation;
    }

    public Reservation checkOut(long id) {
        Reservation reservation = reservations.get(id);
        if (reservation != null && reservation.getStatus() == ReservationStatus.CHECKED_IN) {
            reservation.setStatus(ReservationStatus.CHECKED_OUT);
        }
        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = reservations.get(id);
        if (reservation != null &&
            (reservation.getStatus() == ReservationStatus.PENDING ||
             reservation.getStatus() == ReservationStatus.CONFIRMED)) {

            ReservationStatus previousStatus = reservation.getStatus();
            reservation.setStatus(ReservationStatus.CANCELLED);

            if (previousStatus == ReservationStatus.CONFIRMED) {
                if (reservation.getRatePlan() == RatePlan.STANDARD) {
                    reservation.setRefund(reservation.getAmountPaid());
                } else {
                    reservation.setRefund(BigDecimal.ZERO);
                }
            } else {
                reservation.setRefund(BigDecimal.ZERO);
            }
        }
        return reservation;
    }

    public boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        for (Reservation res : reservations.values()) {
            if (res.getRoomNumber() == roomNumber && res.getStatus() != ReservationStatus.CANCELLED) {
                LocalDate resCheckIn = res.getCheckIn();
                LocalDate resCheckOut = res.getCheckOut();

                if (checkIn.isBefore(resCheckOut) && checkOut.isAfter(resCheckIn)) {
                    return false;
                }
            }
        }
        return true;
    }

    public Collection<Reservation> getAllReservations() {
        return reservations.values();
    }
}

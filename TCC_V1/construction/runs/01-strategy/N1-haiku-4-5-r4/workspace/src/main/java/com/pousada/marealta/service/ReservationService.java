package com.pousada.marealta.service;

import com.pousada.marealta.model.RatePlan;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.ReservationStatus;
import com.pousada.marealta.model.Room;
import com.pousada.marealta.persistence.ReservationDao;
import com.pousada.marealta.persistence.RoomDao;
import com.pousada.marealta.util.MoneyRounder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReservationService {
    private final ReservationDao reservationDao;
    private final RoomDao roomDao;
    private final PricingService pricingService;
    private final MessageService messageService;

    public ReservationService(ReservationDao reservationDao, RoomDao roomDao,
                            PricingService pricingService, MessageService messageService) {
        this.reservationDao = reservationDao;
        this.roomDao = roomDao;
        this.pricingService = pricingService;
        this.messageService = messageService;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests,
                                        RatePlan ratePlan, String companyCode) throws Exception {

        Room room = roomDao.findByNumber(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        if (guests < 1 || guests > room.getCapacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        validateRatePlan(ratePlan, checkIn, checkOut, companyCode);

        List<Reservation> conflicts = reservationDao.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        PricingService.PriceCalculation pricing = pricingService.calculatePrice(room, checkIn, checkOut, ratePlan, companyCode);

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setGuestCpf(guestCpf);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(ratePlan);
        reservation.setCompanyCode(companyCode);
        reservation.setNights(pricing.nights);
        reservation.setSubtotal(pricing.subtotal);
        reservation.setDiscount(pricing.discount);
        reservation.setTotal(pricing.total);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setAmountPaid(BigDecimal.ZERO);

        return reservationDao.save(reservation);
    }

    public Reservation getReservation(long id) throws Exception {
        Reservation reservation = reservationDao.findById(id);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }
        return reservation;
    }

    public Reservation confirmReservation(long id, BigDecimal amountPaid) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(amountPaid);
        reservationDao.update(reservation);

        messageService.sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation checkIn(long id) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationDao.update(reservation);

        return reservation;
    }

    public Reservation checkOut(long id) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationDao.update(reservation);

        return reservation;
    }

    public Reservation cancelReservation(long id) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        BigDecimal refund = PricingService.calculateRefund(reservation.getRatePlan(), reservation.getAmountPaid());
        refund = MoneyRounder.round(refund);

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refund);
        reservationDao.update(reservation);

        return reservation;
    }

    private void validateRatePlan(RatePlan ratePlan, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        switch (ratePlan) {
            case CORPORATE:
                if (!"TECNOSUL".equals(companyCode) && !"AGROVALE".equals(companyCode) && !"FARMAVIDA".equals(companyCode)) {
                    throw new IllegalArgumentException("INVALID_REQUEST");
                }
                break;

            case WEEKEND_PACKAGE:
                if (!PricingService.isWeekendPackageValid(checkIn, checkOut)) {
                    throw new IllegalArgumentException("INVALID_REQUEST");
                }
                break;

            case LONG_STAY:
                long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
                if (nights < 7) {
                    throw new IllegalArgumentException("INVALID_REQUEST");
                }
                break;

            default:
                break;
        }
    }
}

package com.pousadameraalta.service;

import com.pousadameraalta.model.*;
import com.pousadameraalta.repository.DataStore;
import com.pousadameraalta.util.PriceCalculator;
import com.pousadameraalta.util.DocumentGenerator;
import com.pousadameraalta.util.Formatter;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationService {
    private final DataStore dataStore = DataStore.getInstance();

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                         String guestCpf, String companyCnpj, String companyName,
                                         String guestPassport, String guestCountry,
                                         LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan)
            throws Exception {

        Room room = dataStore.getRoom(roomNumber);
        if (room == null) {
            throw new Exception("ROOM_NOT_FOUND");
        }

        if (guests < 1 || guests > room.getCapacity()) {
            throw new Exception(guests > room.getCapacity() ? "CAPACITY_EXCEEDED" : "INVALID_REQUEST");
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new Exception("INVALID_REQUEST");
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new Exception("ROOM_UNAVAILABLE");
        }

        int nights = PriceCalculator.calculateNights(checkIn, checkOut);

        BigDecimal nightPrice = PriceCalculator.calculateNightPrice(room.getNightlyRate(), checkIn);
        BigDecimal subtotal = PriceCalculator.calculateSubtotal(nightPrice, nights);
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, ratePlan == RatePlan.NON_REFUNDABLE);
        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
                dataStore.getNextReservationId(),
                roomNumber,
                guestName,
                guestEmail,
                guestCpf,
                companyCnpj,
                companyName,
                guestPassport,
                guestCountry,
                checkIn,
                checkOut,
                guests,
                ratePlan,
                nights,
                subtotal,
                discount,
                total
        );

        return dataStore.createReservation(reservation);
    }

    public Reservation confirmReservation(long id) throws Exception {
        Reservation reservation = dataStore.getReservation(id);
        if (reservation == null) {
            throw new Exception("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new Exception("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());

        String document = getDocument(reservation);
        String emailBody = generateConfirmationEmail(reservation, document);

        dataStore.saveMessage("EMAIL", reservation.getGuestEmail(), "Reserva confirmada - Pousada Maré Alta", emailBody);

        return dataStore.saveReservation(reservation);
    }

    public Reservation cancelReservation(long id) throws Exception {
        Reservation reservation = dataStore.getReservation(id);
        if (reservation == null) {
            throw new Exception("RESERVATION_NOT_FOUND");
        }

        ReservationStatus status = reservation.getStatus();
        if (status == ReservationStatus.CHECKED_IN || status == ReservationStatus.CHECKED_OUT || status == ReservationStatus.CANCELLED) {
            throw new Exception("OPERATION_NOT_ALLOWED");
        }

        BigDecimal refundAmount = BigDecimal.ZERO;
        if (status == ReservationStatus.CONFIRMED && reservation.getRatePlan() == RatePlan.STANDARD) {
            refundAmount = reservation.getAmountPaid();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refundAmount);

        return dataStore.saveReservation(reservation);
    }

    public Reservation checkIn(long id) throws Exception {
        Reservation reservation = dataStore.getReservation(id);
        if (reservation == null) {
            throw new Exception("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new Exception("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return dataStore.saveReservation(reservation);
    }

    public Reservation checkOut(long id) throws Exception {
        Reservation reservation = dataStore.getReservation(id);
        if (reservation == null) {
            throw new Exception("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new Exception("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return dataStore.saveReservation(reservation);
    }

    public Reservation getReservation(long id) throws Exception {
        Reservation reservation = dataStore.getReservation(id);
        if (reservation == null) {
            throw new Exception("RESERVATION_NOT_FOUND");
        }
        return reservation;
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return dataStore.getAllReservations().stream()
                .filter(r -> r.getRoomNumber() == roomNumber && r.getStatus() != ReservationStatus.CANCELLED)
                .anyMatch(r -> {
                    LocalDate rCheckOut = r.getCheckOut();
                    LocalDate rCheckIn = r.getCheckIn();
                    return !(checkOut.isBefore(rCheckIn) || checkIn.isAfter(rCheckOut) || checkIn.isEqual(rCheckOut));
                });
    }

    private String getDocument(Reservation reservation) {
        if (reservation.getGuestCpf() != null) {
            return DocumentGenerator.generateReceipt(reservation);
        } else if (reservation.getCompanyCnpj() != null) {
            return DocumentGenerator.generateInvoice(reservation);
        } else {
            return DocumentGenerator.generateBookingReceipt(reservation);
        }
    }

    private String generateConfirmationEmail(Reservation reservation, String document) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(document).append("\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!\n");
        return sb.toString();
    }
}

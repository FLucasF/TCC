package com.pousada.service;

import com.pousada.dao.ReservationDAO;
import com.pousada.dao.RoomDAO;
import com.pousada.model.Reservation;
import com.pousada.model.Room;
import com.pousada.util.DocumentFormatter;
import com.pousada.util.MoneyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public class ReservationService {
    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);
    private static final ReservationDAO reservationDAO = new ReservationDAO();
    private static final RoomDAO roomDAO = new RoomDAO();

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                         String guestCpf, String companyCnpj, String companyName,
                                         String guestPassport, String guestCountry,
                                         LocalDate checkIn, LocalDate checkOut, int guests,
                                         String ratePlan) {

        Optional<Room> roomOpt = roomDAO.findByNumber(roomNumber);
        if (roomOpt.isEmpty()) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        Room room = roomOpt.get();

        // Validate dates
        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        // Validate guests
        if (guests < 1) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (guests > room.getCapacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        // Check room availability
        var conflictingReservations = reservationDAO.findByRoomAndDates(roomNumber, checkIn, checkOut);
        if (!conflictingReservations.isEmpty()) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        // Calculate nights and prices
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

        BigDecimal subtotal = MoneyUtil.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        BigDecimal discount = MoneyUtil.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = MoneyUtil.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
            0, roomNumber, guestName, guestEmail,
            guestCpf, companyCnpj, companyName,
            guestPassport, guestCountry,
            checkIn, checkOut, guests, ratePlan,
            nights, subtotal, discount, total,
            "PENDING", BigDecimal.ZERO, null
        );

        int id = reservationDAO.create(reservation);
        reservation = new Reservation(
            id, roomNumber, guestName, guestEmail,
            guestCpf, companyCnpj, companyName,
            guestPassport, guestCountry,
            checkIn, checkOut, guests, ratePlan,
            nights, subtotal, discount, total,
            "PENDING", BigDecimal.ZERO, null
        );

        return reservation;
    }

    public Reservation getReservation(int id) {
        Optional<Reservation> reservationOpt = reservationDAO.findById(id);
        if (reservationOpt.isEmpty()) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }
        return reservationOpt.get();
    }

    public Reservation confirmReservation(int id, BigDecimal amountPaid) {
        Reservation reservation = getReservation(id);

        if (!"PENDING".equals(reservation.getStatus())) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus("CONFIRMED");
        reservation.setAmountPaid(amountPaid);

        reservationDAO.update(reservation);

        // Send confirmation email
        Optional<Room> roomOpt = roomDAO.findByNumber(reservation.getRoomNumber());
        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();
            String emailBody = formatConfirmationEmail(reservation, room);
            MessageService.sendEmail(reservation.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta", emailBody);
        }

        return reservation;
    }

    public Reservation cancelReservation(int id) {
        Reservation reservation = getReservation(id);

        if (!("PENDING".equals(reservation.getStatus()) || "CONFIRMED".equals(reservation.getStatus()))) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        BigDecimal refund = BigDecimal.ZERO;

        if ("CONFIRMED".equals(reservation.getStatus())) {
            if ("STANDARD".equals(reservation.getRatePlan())) {
                refund = reservation.getAmountPaid();
            }
            // For NON_REFUNDABLE, no refund
        }

        reservation.setStatus("CANCELLED");
        reservation.setRefund(refund);

        reservationDAO.update(reservation);

        return reservation;
    }

    public Reservation checkIn(int id) {
        Reservation reservation = getReservation(id);

        if (!"CONFIRMED".equals(reservation.getStatus())) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus("CHECKED_IN");
        reservationDAO.update(reservation);

        return reservation;
    }

    public Reservation checkOut(int id) {
        Reservation reservation = getReservation(id);

        if (!"CHECKED_IN".equals(reservation.getStatus())) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus("CHECKED_OUT");
        reservationDAO.update(reservation);

        return reservation;
    }

    private String formatConfirmationEmail(Reservation reservation, Room room) {
        String document;
        if (reservation.getGuestCpf() != null) {
            document = DocumentFormatter.formatReceipt(reservation, room);
        } else if (reservation.getCompanyCnpj() != null) {
            document = DocumentFormatter.formatInvoice(reservation, room);
        } else {
            document = DocumentFormatter.formatBookingReceipt(reservation, room);
        }

        return String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.getGuestName(), document
        );
    }
}

package com.pousadamareaalta.service;

import com.pousadamareaalta.exception.*;
import com.pousadamareaalta.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class ReservationService {
    private final Map<Long, Reservation> reservations;
    private final RoomService roomService;
    private final PricingService pricingService;
    private final MessageService messageService;
    private final ReceiptFormatter receiptFormatter;
    private long nextReservationId = 1;

    public ReservationService(RoomService roomService, PricingService pricingService,
                             MessageService messageService) {
        this.reservations = new LinkedHashMap<>();
        this.roomService = roomService;
        this.pricingService = pricingService;
        this.messageService = messageService;
        this.receiptFormatter = new ReceiptFormatter();
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
        Room room = roomService.getRoom(roomNumber);
        if (room == null) {
            throw new RoomNotFound();
        }

        if (guests < 1 || guests > room.getCapacity()) {
            throw new CapacityExceeded();
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequest("Checkout must be after checkin");
        }

        List<LocalDate> nights = getNights(checkIn, checkOut);
        if (isRoomBooked(roomNumber, nights)) {
            throw new RoomUnavailable();
        }

        BigDecimal[] nightPrices = new BigDecimal[nights.size()];
        for (int i = 0; i < nights.size(); i++) {
            nightPrices[i] = pricingService.calculateNightPrice(room.getNightlyRate(), nights.get(i));
        }

        BigDecimal subtotal = pricingService.calculateSubtotal(nightPrices);
        BigDecimal discount = pricingService.calculateDiscount(subtotal, ratePlan == RatePlan.NON_REFUNDABLE);
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
            nextReservationId++,
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            ratePlan,
            nights.size(),
            subtotal,
            discount,
            total
        );

        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Reservation getReservation(long id) {
        Reservation res = reservations.get(id);
        if (res == null) {
            throw new ReservationNotFound();
        }
        return res;
    }

    public Reservation confirmReservation(long id, BigDecimal amountPaid) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowed();
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(amountPaid);

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowed();
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservation;
    }

    public Receipt checkOut(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowed();
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return createReceipt(reservation);
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowed();
        }

        BigDecimal refund = BigDecimal.ZERO;
        if (reservation.getStatus() == ReservationStatus.CONFIRMED &&
            reservation.getRatePlan() == RatePlan.STANDARD) {
            refund = reservation.getAmountPaid();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refund);

        return reservation;
    }

    public Receipt getDocument(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.PENDING ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new OperationNotAllowed();
        }

        return createReceipt(reservation);
    }

    public List<Reservation> getAllReservations() {
        return new ArrayList<>(reservations.values());
    }

    private void sendConfirmationEmail(Reservation reservation) {
        Receipt receipt = createReceipt(reservation);
        String receiptText = receiptFormatter.format(receipt);

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format("Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.getGuestName(),
            receiptText);

        messageService.sendEmail(reservation.getGuestEmail(), subject, body);
    }

    private Receipt createReceipt(Reservation reservation) {
        Room room = roomService.getRoom(reservation.getRoomNumber());
        return new Receipt(
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            room.getType().toString(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }

    private boolean isRoomBooked(int roomNumber, List<LocalDate> nights) {
        for (Reservation res : reservations.values()) {
            if (res.getRoomNumber() != roomNumber ||
                res.getStatus() == ReservationStatus.CANCELLED) {
                continue;
            }

            List<LocalDate> resNights = getNights(res.getCheckIn(), res.getCheckOut());
            for (LocalDate night : nights) {
                if (resNights.contains(night)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<LocalDate> getNights(LocalDate checkIn, LocalDate checkOut) {
        List<LocalDate> nights = new ArrayList<>();
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            nights.add(current);
            current = current.plusDays(1);
        }
        return nights;
    }
}

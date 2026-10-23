package com.pousadamarealta.service;

import com.pousadamarealta.database.ReservationDAO;
import com.pousadamarealta.domain.*;
import com.pousadamarealta.exception.*;
import com.pousadamarealta.messaging.MessageService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ReservationService {
    private final ReservationDAO reservationDAO;
    private final RoomService roomService;
    private final PricingService pricingService;
    private final MessageService messageService;

    public ReservationService(ReservationDAO reservationDAO, RoomService roomService,
                            PricingService pricingService, MessageService messageService) {
        this.reservationDAO = reservationDAO;
        this.roomService = roomService;
        this.pricingService = pricingService;
        this.messageService = messageService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
        Room room = roomService.getRoomByNumber(roomNumber);

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (reservationDAO.hasConflictingReservation(roomNumber, checkIn, checkOut, null)) {
            throw new RoomUnavailableException("Room not available for selected dates");
        }

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setGuestCpf(guestCpf);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(ratePlan);

        int nights = (int) checkIn.until(checkOut).getDays();
        reservation.setNights(nights);

        BigDecimal subtotal = pricingService.calculateSubtotal(room.getType(), checkIn, checkOut);
        reservation.setSubtotal(subtotal);

        BigDecimal discount = pricingService.calculateDiscount(subtotal, ratePlan);
        reservation.setDiscount(discount);

        BigDecimal total = pricingService.calculateTotal(subtotal, discount);
        reservation.setTotal(total);

        return reservationDAO.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationDAO.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Only pending reservations can be confirmed");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationDAO.update(reservation);

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Only confirmed reservations can be checked in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationDAO.update(reservation);

        return reservation;
    }

    public Map<String, Object> checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Only checked-in reservations can be checked out");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationDAO.update(reservation);

        return generateDocument(reservation);
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Only pending or confirmed reservations can be cancelled");
        }

        BigDecimal refundAmount = BigDecimal.ZERO;
        if (reservation.getRatePlan() == RatePlan.STANDARD &&
            reservation.getStatus() == ReservationStatus.CONFIRMED) {
            refundAmount = reservation.getAmountPaid();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refundAmount);
        reservationDAO.update(reservation);

        return reservation;
    }

    public Map<String, Object> getDocument(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Document only available for paid reservations");
        }

        return generateDocument(reservation);
    }

    private void sendConfirmationEmail(Reservation reservation) {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        String receipt = formatReceipt(reservation, room);

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """, reservation.getGuestName(), receipt);

        messageService.sendMessage(MessageChannel.EMAIL, reservation.getGuestEmail(), subject, body);
    }

    private Map<String, Object> generateDocument(Reservation reservation) {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());

        Map<String, Object> document = new java.util.LinkedHashMap<>();
        document.put("title", "Comprovante de reserva");
        document.put("reservationId", reservation.getId());
        document.put("guestName", reservation.getGuestName());
        document.put("guestCpf", reservation.getGuestCpf());
        document.put("roomNumber", reservation.getRoomNumber());
        document.put("roomType", room.getType().name());
        document.put("checkIn", reservation.getCheckIn().toString());
        document.put("checkOut", reservation.getCheckOut().toString());
        document.put("nights", reservation.getNights());
        document.put("subtotal", reservation.getSubtotal());
        document.put("discount", reservation.getDiscount());
        document.put("total", reservation.getTotal());
        return document;
    }

    private String formatReceipt(Reservation reservation, Room room) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        StringBuilder receipt = new StringBuilder();
        receipt.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        receipt.append(String.format("Hóspede: %s (CPF %s)\n",
            reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));
        receipt.append(String.format("Quarto: %s (%s)\n",
            reservation.getRoomNumber(), room.getType().name()));
        receipt.append(String.format("Entrada: %s | Saída: %s | %d %s\n",
            reservation.getCheckIn().format(dateFormatter),
            reservation.getCheckOut().format(dateFormatter),
            reservation.getNights(),
            reservation.getNights() == 1 ? "diária" : "diárias"));
        receipt.append(String.format("Diárias: %s\n", formatMoney(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("Desconto: %s\n", formatMoney(reservation.getDiscount())));
        }

        receipt.append(String.format("Total: %s", formatMoney(reservation.getTotal())));

        return receipt.toString();
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return String.format("%s.%s.%s-%s",
                cpf.substring(0, 3),
                cpf.substring(3, 6),
                cpf.substring(6, 9),
                cpf.substring(9, 11));
        }
        return cpf;
    }
}

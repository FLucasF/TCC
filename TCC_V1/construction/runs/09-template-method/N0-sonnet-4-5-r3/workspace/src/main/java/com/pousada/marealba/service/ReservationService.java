package com.pousada.marealba.service;

import com.pousada.marealba.exception.ApiException;
import com.pousada.marealba.model.*;
import com.pousada.marealba.repository.ReservationRepository;
import com.pousada.marealba.util.CpfUtil;
import com.pousada.marealba.util.DateUtil;
import com.pousada.marealba.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private final MessageService messageService;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomService roomService,
                            MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
        this.messageService = messageService;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                       String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                       int guests, RatePlan ratePlan) {

        Room room = roomService.getRoomByNumber(roomNumber);

        if (guests < 1) {
            throw ApiException.invalidRequest("Number of guests must be at least 1");
        }

        if (guests > room.getCapacity()) {
            throw ApiException.capacityExceeded();
        }

        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.invalidRequest("Check-out date must be after check-in date");
        }

        List<Reservation> conflicts = reservationRepository.findConflicting(roomNumber, checkIn, checkOut, null);
        if (!conflicts.isEmpty()) {
            throw ApiException.roomUnavailable();
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

        calculatePricing(reservation, room);

        return reservationRepository.save(reservation);
    }

    private void calculatePricing(Reservation reservation, Room room) {
        int nights = (int) ChronoUnit.DAYS.between(reservation.getCheckIn(), reservation.getCheckOut());
        reservation.setNights(nights);

        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentDate = reservation.getCheckIn();

        for (int i = 0; i < nights; i++) {
            double baseRate = room.getNightlyRate();
            if (DateUtil.isHighSeason(currentDate)) {
                baseRate = baseRate * 1.20;
            }
            BigDecimal nightRate = MoneyUtil.round(baseRate);
            subtotal = subtotal.add(nightRate);
            currentDate = currentDate.plusDays(1);
        }

        subtotal = MoneyUtil.round(subtotal);
        reservation.setSubtotal(subtotal);

        BigDecimal discount = BigDecimal.ZERO;
        if (reservation.getRatePlan().getDiscount() > 0) {
            discount = subtotal.multiply(BigDecimal.valueOf(reservation.getRatePlan().getDiscount()));
            discount = MoneyUtil.round(discount);
        }
        reservation.setDiscount(discount);

        BigDecimal total = subtotal.subtract(discount);
        total = MoneyUtil.round(total);
        reservation.setTotal(total);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(ApiException::reservationNotFound);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw ApiException.operationNotAllowed("Only pending reservations can be confirmed");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.update(reservation);

        sendConfirmationEmail(reservation);

        return reservation;
    }

    private void sendConfirmationEmail(Reservation reservation) {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        String receipt = generateReceiptText(reservation, room);

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """, reservation.getGuestName(), receipt);

        messageService.sendEmail(reservation.getGuestEmail(), subject, body);
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
            reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw ApiException.operationNotAllowed("Cannot cancel reservation after check-in");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw ApiException.operationNotAllowed("Reservation is already cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        BigDecimal refund = BigDecimal.ZERO;
        if (reservation.getRatePlan() == RatePlan.STANDARD &&
            reservation.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            refund = reservation.getAmountPaid();
        }
        reservation.setRefund(refund);

        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw ApiException.operationNotAllowed("Only confirmed reservations can check in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Document checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw ApiException.operationNotAllowed("Only checked-in reservations can check out");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.update(reservation);

        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        return generateDocument(reservation, room);
    }

    public Document getDocument(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED &&
            reservation.getStatus() != ReservationStatus.CHECKED_IN &&
            reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            throw ApiException.operationNotAllowed("Document is only available for paid reservations");
        }

        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        return generateDocument(reservation, room);
    }

    private Document generateDocument(Reservation reservation, Room room) {
        Document document = new Document();
        document.setTitle("Comprovante de reserva");
        document.setReservationId(reservation.getId());
        document.setGuestName(reservation.getGuestName());
        document.setGuestCpf(reservation.getGuestCpf());
        document.setRoomNumber(reservation.getRoomNumber());
        document.setRoomType(room.getType());
        document.setCheckIn(reservation.getCheckIn());
        document.setCheckOut(reservation.getCheckOut());
        document.setNights(reservation.getNights());
        document.setSubtotal(reservation.getSubtotal());
        document.setDiscount(reservation.getDiscount());
        document.setTotal(reservation.getTotal());
        return document;
    }

    private String generateReceiptText(Reservation reservation, Room room) {
        StringBuilder receipt = new StringBuilder();
        receipt.append(String.format("Comprovante de reserva nº %d%n", reservation.getId()));
        receipt.append(String.format("Hóspede: %s (CPF %s)%n",
            reservation.getGuestName(),
            CpfUtil.format(reservation.getGuestCpf())));
        receipt.append(String.format("Quarto: %d (%s)%n",
            reservation.getRoomNumber(),
            room.getType().name()));
        receipt.append(String.format("Entrada: %s | Saída: %s | %d %s%n",
            DateUtil.formatDisplay(reservation.getCheckIn()),
            DateUtil.formatDisplay(reservation.getCheckOut()),
            reservation.getNights(),
            reservation.getNights() == 1 ? "diária" : "diárias"));
        receipt.append(String.format("Diárias: %s%n",
            MoneyUtil.formatBrazilian(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("Desconto: %s%n",
                MoneyUtil.formatBrazilian(reservation.getDiscount())));
        }

        receipt.append(String.format("Total: %s",
            MoneyUtil.formatBrazilian(reservation.getTotal())));

        return receipt.toString();
    }
}

package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final PricingService pricingService;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomRepository roomRepository,
                            MessageRepository messageRepository,
                            PricingService pricingService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.pricingService = pricingService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException(roomNumber));

        if (guests < 1) {
            throw new InvalidRequestException("Number of guests must be at least 1");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException(guests, room.getCapacity());
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        if (!isRoomAvailable(roomNumber, checkIn, checkOut, null)) {
            throw new RoomUnavailableException(roomNumber);
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

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        reservation.setNights(nights);

        BigDecimal subtotal = pricingService.calculateSubtotal(room, checkIn, checkOut);
        reservation.setSubtotal(subtotal);

        BigDecimal discount = pricingService.calculateDiscount(subtotal, ratePlan);
        reservation.setDiscount(discount);

        BigDecimal total = pricingService.calculateTotal(subtotal, discount);
        reservation.setTotal(total);

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Cannot confirm reservation in status " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.update(reservation);

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN
            || reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new OperationNotAllowedException("Cannot cancel reservation in status " + reservation.getStatus());
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new OperationNotAllowedException("Reservation is already cancelled");
        }

        BigDecimal refundAmount = calculateRefund(reservation);

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refundAmount);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Cannot check-in reservation in status " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Cannot check-out reservation in status " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.update(reservation);

        return reservation;
    }

    private BigDecimal calculateRefund(Reservation reservation) {
        if (reservation.getRatePlan() == RatePlan.NON_REFUNDABLE) {
            return BigDecimal.ZERO;
        }
        return reservation.getAmountPaid();
    }

    private void sendConfirmationEmail(Reservation reservation) {
        String document = generateDocument(reservation);
        String body = String.format("Olá, %s!\n\n" +
                "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                "%s\n\n" +
                "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                "Até breve!", reservation.getGuestName(), document);

        Message message = new Message("EMAIL", reservation.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta", body);
        messageRepository.save(message);
    }

    public String generateDocument(Reservation reservation) {
        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException("Document is only available for paid reservations");
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        StringBuilder doc = new StringBuilder();
        doc.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        doc.append("Hóspede: ").append(reservation.getGuestName())
           .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        doc.append("Quarto: ").append(reservation.getRoomNumber())
           .append(" (").append(room.getType()).append(")\n");
        doc.append("Entrada: ").append(formatDate(reservation.getCheckIn()))
           .append(" | Saída: ").append(formatDate(reservation.getCheckOut()))
           .append(" | ").append(reservation.getNights())
           .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        doc.append("Diárias: ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            doc.append("Desconto: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        doc.append("Total: ").append(formatMoney(reservation.getTotal()));

        return doc.toString();
    }

    private boolean isRoomAvailable(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        List<Reservation> conflicting = reservationRepository.findConflicting(roomNumber, checkIn, checkOut);

        if (excludeReservationId != null) {
            conflicting.removeIf(r -> r.getId().equals(excludeReservationId));
        }

        return conflicting.isEmpty();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "."
                 + cpf.substring(6, 9) + "-" + cpf.substring(9, 11);
        }
        return cpf;
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d",
            date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }
}

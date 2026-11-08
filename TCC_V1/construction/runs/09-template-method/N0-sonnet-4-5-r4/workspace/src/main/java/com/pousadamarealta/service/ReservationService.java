package com.pousadamarealta.service;

import com.pousadamarealta.model.*;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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
            .orElseThrow(() -> new RoomNotFoundException());

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        var conflicts = reservationRepository.findConflicting(roomNumber, checkIn, checkOut, null);
        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException();
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
            BigDecimal nightPrice = pricingService.calculateNightPrice(room.getNightlyRate(), currentDate);
            subtotal = subtotal.add(nightPrice);
            currentDate = currentDate.plusDays(1);
        }

        subtotal = pricingService.roundBankers(subtotal);
        reservation.setSubtotal(subtotal);

        BigDecimal discount = BigDecimal.ZERO;
        if (reservation.getRatePlan() == RatePlan.NON_REFUNDABLE) {
            discount = pricingService.roundBankers(subtotal.multiply(reservation.getRatePlan().getDiscountRate()));
        }
        reservation.setDiscount(discount);

        BigDecimal total = pricingService.roundBankers(subtotal.subtract(discount));
        reservation.setTotal(total);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.save(reservation);

        sendConfirmationEmail(reservation);

        return reservation;
    }

    private void sendConfirmationEmail(Reservation reservation) {
        String receipt = generateReceipt(reservation);
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", reservation.getGuestName(), receipt);

        Message message = new Message(MessageChannel.EMAIL, reservation.getGuestEmail(), subject, body);
        messageRepository.save(message);
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        BigDecimal refundAmount = BigDecimal.ZERO;
        if (reservation.getRatePlan() == RatePlan.STANDARD) {
            refundAmount = reservation.getAmountPaid();
        }
        reservation.setRefund(refundAmount);

        return reservationRepository.save(reservation);
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException());

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return reservationRepository.save(reservation);
    }

    public String generateReceipt(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.PENDING) {
            throw new OperationNotAllowedException();
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new RoomNotFoundException());

        StringBuilder receipt = new StringBuilder();
        receipt.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        receipt.append(String.format("Hóspede: %s (CPF %s)\n",
            reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));
        receipt.append(String.format("Quarto: %s (%s)\n",
            reservation.getRoomNumber(), room.getType().name()));
        receipt.append(String.format("Entrada: %s | Saída: %s | %d %s\n",
            formatDate(reservation.getCheckIn()),
            formatDate(reservation.getCheckOut()),
            reservation.getNights(),
            reservation.getNights() == 1 ? "diária" : "diárias"));
        receipt.append(String.format("Diárias: %s\n", formatMoney(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("Desconto: %s\n", formatMoney(reservation.getDiscount())));
        }

        receipt.append(String.format("Total: %s", formatMoney(reservation.getTotal())));

        return receipt.toString();
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

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d",
            date.getDayOfMonth(),
            date.getMonthValue(),
            date.getYear());
    }

    private String formatMoney(BigDecimal amount) {
        String formatted = String.format("%,.2f", amount);
        formatted = formatted.replace(',', 'X').replace('.', ',').replace('X', '.');
        return "R$ " + formatted;
    }

    public static class RoomNotFoundException extends RuntimeException {
    }

    public static class ReservationNotFoundException extends RuntimeException {
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }

    public static class CapacityExceededException extends RuntimeException {
    }

    public static class RoomUnavailableException extends RuntimeException {
    }

    public static class OperationNotAllowedException extends RuntimeException {
    }
}

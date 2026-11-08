package com.pousadamarealta.service;

import com.pousadamarealta.dto.ReceiptResponse;
import com.pousadamarealta.model.*;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final PricingService pricingService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
                                        int guests, RatePlan ratePlan, String companyCode) {

        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException("Room not found"));

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Number of guests exceeds room capacity");
        }

        if (guests < 1) {
            throw new InvalidRequestException("Number of guests must be at least 1");
        }

        pricingService.validateRatePlan(ratePlan, checkIn, checkOut, companyCode);

        List<Reservation> conflicts = reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        PricingService.PricingResult pricing = pricingService.calculatePrice(room, checkIn, checkOut, ratePlan, companyCode);

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

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Can only confirm pending reservations");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.update(reservation);

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Can only cancel pending or confirmed reservations");
        }

        BigDecimal refund = pricingService.calculateRefund(reservation.getRatePlan(), reservation.getAmountPaid());

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refund);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Can only check-in confirmed reservations");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.update(reservation);

        return reservation;
    }

    public ReceiptResponse checkOut(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Can only check-out guests who have checked in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.update(reservation);

        return generateReceipt(reservation);
    }

    public ReceiptResponse getDocument(Long id) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED &&
            reservation.getStatus() != ReservationStatus.CHECKED_IN &&
            reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            throw new OperationNotAllowedException("Document only available for paid reservations");
        }

        return generateReceipt(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));
    }

    private ReceiptResponse generateReceipt(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        ReceiptResponse receipt = new ReceiptResponse();
        receipt.setTitle("Comprovante de reserva");
        receipt.setReservationId(reservation.getId());
        receipt.setGuestName(reservation.getGuestName());
        receipt.setGuestCpf(formatCpf(reservation.getGuestCpf()));
        receipt.setRoomNumber(reservation.getRoomNumber());
        receipt.setRoomType(room.getType().name());
        receipt.setCheckIn(reservation.getCheckIn().format(DATE_FORMATTER));
        receipt.setCheckOut(reservation.getCheckOut().format(DATE_FORMATTER));
        receipt.setNights(reservation.getNights());
        receipt.setSubtotal(reservation.getSubtotal());
        receipt.setDiscount(reservation.getDiscount());
        receipt.setTotal(reservation.getTotal());

        return receipt;
    }

    private void sendConfirmationEmail(Reservation reservation) {
        ReceiptResponse receipt = generateReceipt(reservation);
        String receiptText = formatReceiptText(receipt);

        String emailBody = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""",
            reservation.getGuestName(),
            receiptText
        );

        Message message = new Message(
            "EMAIL",
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            emailBody
        );

        messageRepository.save(message);
    }

    private String formatReceiptText(ReceiptResponse receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d%n", receipt.getReservationId()));
        sb.append(String.format("Hóspede: %s (CPF %s)%n", receipt.getGuestName(), receipt.getGuestCpf()));
        sb.append(String.format("Quarto: %s (%s)%n", receipt.getRoomNumber(), receipt.getRoomType()));

        String nightsText = receipt.getNights() == 1 ? "1 diária" : receipt.getNights() + " diárias";
        sb.append(String.format("Entrada: %s | Saída: %s | %s%n",
            receipt.getCheckIn(), receipt.getCheckOut(), nightsText));

        sb.append(String.format("Diárias: %s%n", formatMoney(receipt.getSubtotal())));

        if (receipt.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s%n", formatMoney(receipt.getDiscount())));
        }

        sb.append(String.format("Total: %s", formatMoney(receipt.getTotal())));

        return sb.toString();
    }

    private String formatMoney(BigDecimal value) {
        return String.format("R$ %,.2f", value).replace(',', '#').replace('.', ',').replace('#', '.');
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return String.format("%s.%s.%s-%s",
                cpf.substring(0, 3),
                cpf.substring(3, 6),
                cpf.substring(6, 9),
                cpf.substring(9, 11)
            );
        }
        return cpf;
    }

    public static class RoomNotFoundException extends RuntimeException {
        public RoomNotFoundException(String message) {
            super(message);
        }
    }

    public static class ReservationNotFoundException extends RuntimeException {
        public ReservationNotFoundException(String message) {
            super(message);
        }
    }

    public static class RoomUnavailableException extends RuntimeException {
        public RoomUnavailableException(String message) {
            super(message);
        }
    }

    public static class CapacityExceededException extends RuntimeException {
        public CapacityExceededException(String message) {
            super(message);
        }
    }

    public static class OperationNotAllowedException extends RuntimeException {
        public OperationNotAllowedException(String message) {
            super(message);
        }
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }
}

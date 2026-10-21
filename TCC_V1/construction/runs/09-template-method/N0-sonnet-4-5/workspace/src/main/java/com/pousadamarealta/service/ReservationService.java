package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.exception.*;
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

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail, String guestCpf,
                                         LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            throw new InvalidRequestException("Check-out date must be after check-in date");
        }

        if (guests < 1) {
            throw new InvalidRequestException("Number of guests must be at least 1");
        }

        Room room = roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException("Room " + roomNumber + " not found"));

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room " + roomNumber + " has capacity for " + room.getCapacity() + " guests");
        }

        if (!reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut).isEmpty()) {
            throw new RoomUnavailableException("Room " + roomNumber + " is not available for the selected dates");
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        BigDecimal discount = calculateDiscount(subtotal, ratePlan);
        BigDecimal total = pricingService.round(subtotal.subtract(discount));

        long id = reservationRepository.nextId();
        Reservation reservation = new Reservation(id, roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total);

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Only pending reservations can be confirmed");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
            reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new OperationNotAllowedException("Cannot cancel a reservation that has been checked in or is already cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        if (reservation.getRatePlan() == RatePlan.STANDARD && reservation.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            reservation.setRefund(reservation.getAmountPaid());
        } else {
            reservation.setRefund(BigDecimal.ZERO);
        }

        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Only confirmed reservations can be checked in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservation;
    }

    public ReceiptDocument checkOut(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Only checked-in reservations can be checked out");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return createReceipt(reservation);
    }

    public ReceiptDocument getDocument(long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException("Document is only available for paid reservations");
        }

        return createReceipt(reservation);
    }

    public Reservation getReservation(long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation " + id + " not found"));
    }

    private BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate date = checkIn;

        while (date.isBefore(checkOut)) {
            BigDecimal nightPrice = pricingService.calculateNightPrice(baseRate, date);
            subtotal = subtotal.add(nightPrice);
            date = date.plusDays(1);
        }

        return pricingService.round(subtotal);
    }

    private BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        BigDecimal discount = subtotal.multiply(ratePlan.getDiscountRate());
        return pricingService.round(discount);
    }

    private ReceiptDocument createReceipt(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        return new ReceiptDocument(
                "Comprovante de reserva",
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestCpf(),
                reservation.getRoomNumber(),
                room.getType(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal()
        );
    }

    private void sendConfirmationEmail(Reservation reservation) {
        ReceiptDocument receipt = createReceipt(reservation);
        String body = formatConfirmationEmail(receipt);

        long messageId = messageRepository.nextId();
        Message message = new Message(
                messageId,
                "EMAIL",
                reservation.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta",
                body
        );

        messageRepository.save(message);
    }

    private String formatConfirmationEmail(ReceiptDocument receipt) {
        String receiptText = formatReceipt(receipt);

        return String.format("Olá, %s!\n\n" +
                "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                "%s\n\n" +
                "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                "Até breve!",
                receipt.getGuestName(),
                receiptText);
    }

    private String formatReceipt(ReceiptDocument receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d\n", receipt.getReservationId()));
        sb.append(String.format("Hóspede: %s (CPF %s)\n", receipt.getGuestName(), formatCpf(receipt.getGuestCpf())));
        sb.append(String.format("Quarto: %s (%s)\n", receipt.getRoomNumber(), receipt.getRoomType()));
        sb.append(String.format("Entrada: %s | Saída: %s | %d %s\n",
                formatDate(receipt.getCheckIn()),
                formatDate(receipt.getCheckOut()),
                receipt.getNights(),
                receipt.getNights() == 1 ? "diária" : "diárias"));
        sb.append(String.format("Diárias: %s\n", formatMoney(receipt.getSubtotal())));

        if (receipt.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s\n", formatMoney(receipt.getDiscount())));
        }

        sb.append(String.format("Total: %s", formatMoney(receipt.getTotal())));

        return sb.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(',', 'X').replace('.', ',').replace('X', '.');
    }
}

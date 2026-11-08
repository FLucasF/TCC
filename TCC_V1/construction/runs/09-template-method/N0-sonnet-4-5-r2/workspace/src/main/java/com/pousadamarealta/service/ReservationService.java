package com.pousadamarealta.service;

import com.pousadamarealta.model.*;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.util.CpfUtils;
import com.pousadamarealta.util.DateUtils;
import com.pousadamarealta.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final PricingService pricingService;

    public ReservationService() {
        this.reservationRepository = new ReservationRepository();
        this.roomRepository = new RoomRepository();
        this.messageRepository = new MessageRepository();
        this.pricingService = new PricingService();
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {

        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException());

        if (guests < 1 || guests > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        List<Reservation> conflicts = reservationRepository.findByRoomAndDateRange(
            roomNumber, checkIn, checkOut);

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
            .orElseThrow(() -> new ReservationNotFoundException());
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.save(reservation);

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

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
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return reservationRepository.save(reservation);
    }

    public String generateDocument(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();

        StringBuilder doc = new StringBuilder();
        doc.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        doc.append(String.format("Hóspede: %s (CPF %s)\n",
            reservation.getGuestName(),
            CpfUtils.format(reservation.getGuestCpf())));
        doc.append(String.format("Quarto: %s (%s)\n",
            reservation.getRoomNumber(),
            room.getType().name()));
        doc.append(String.format("Entrada: %s | Saída: %s | %d %s\n",
            DateUtils.formatDisplay(reservation.getCheckIn()),
            DateUtils.formatDisplay(reservation.getCheckOut()),
            reservation.getNights(),
            reservation.getNights() == 1 ? "diária" : "diárias"));
        doc.append(String.format("Diárias: %s\n",
            MoneyUtils.formatBRL(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            doc.append(String.format("Desconto: %s\n",
                MoneyUtils.formatBRL(reservation.getDiscount())));
        }

        doc.append(String.format("Total: %s",
            MoneyUtils.formatBRL(reservation.getTotal())));

        return doc.toString();
    }

    private void sendConfirmationEmail(Reservation reservation) {
        String document = generateDocument(reservation);

        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""",
            reservation.getGuestName(),
            document);

        Message message = new Message(
            "EMAIL",
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );

        messageRepository.save(message);
    }
}

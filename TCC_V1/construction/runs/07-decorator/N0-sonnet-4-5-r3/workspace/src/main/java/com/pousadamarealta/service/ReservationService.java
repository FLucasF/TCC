package com.pousadamarealta.service;

import com.pousadamarealta.exception.*;
import com.pousadamarealta.messaging.OutgoingMessage;
import com.pousadamarealta.model.*;
import com.pousadamarealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private final PricingService pricingService;
    private final MessageService messageService;

    public ReservationService(ReservationRepository reservationRepository, RoomService roomService,
                              PricingService pricingService, MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
        this.pricingService = pricingService;
        this.messageService = messageService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail, String guestCpf,
                                         LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Room room = roomService.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomNumber));

        validateReservation(room, checkIn, checkOut, guests, null);

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = pricingService.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        BigDecimal discount = pricingService.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setGuestCpf(guestCpf);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(ratePlan);
        reservation.setNights(nights);
        reservation.setSubtotal(subtotal);
        reservation.setDiscount(discount);
        reservation.setTotal(total);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setAmountPaid(BigDecimal.ZERO);
        reservation.setRefund(null);

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Reservation cannot be confirmed in status: " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.update(reservation);

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Reservation cannot be cancelled in status: " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        BigDecimal refund = BigDecimal.ZERO;
        if (reservation.getRatePlan() == RatePlan.STANDARD) {
            refund = reservation.getAmountPaid();
        }
        reservation.setRefund(refund);

        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Reservation cannot be checked in from status: " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Reservation cannot be checked out from status: " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation findById(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + id));
    }

    public String generateDocument(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.PENDING || reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new OperationNotAllowedException("Cannot generate document for reservation in status: " + reservation.getStatus());
        }

        Room room = roomService.findByNumber(reservation.getRoomNumber()).orElseThrow();
        return buildReceipt(reservation, room);
    }

    private void sendConfirmationEmail(Reservation reservation) {
        Room room = roomService.findByNumber(reservation.getRoomNumber()).orElseThrow();
        String receipt = buildReceipt(reservation, room);

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format("Olá, %s!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n%s\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!",
            reservation.getGuestName(), receipt);

        OutgoingMessage message = new OutgoingMessage(MessageChannel.EMAIL, reservation.getGuestEmail(), subject, body);
        messageService.send(message);
    }

    private String buildReceipt(Reservation reservation, Room room) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String nightsText = reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias";

        StringBuilder receipt = new StringBuilder();
        receipt.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        receipt.append(String.format("Hóspede: %s (CPF %s)\n", reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));
        receipt.append(String.format("Quarto: %s (%s)\n", reservation.getRoomNumber(), room.getType().name()));
        receipt.append(String.format("Entrada: %s | Saída: %s | %s\n",
            reservation.getCheckIn().format(dateFormatter),
            reservation.getCheckOut().format(dateFormatter),
            nightsText));
        receipt.append(String.format("Diárias: %s\n", formatMoney(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("Desconto: %s\n", formatMoney(reservation.getDiscount())));
        }

        receipt.append(String.format("Total: %s", formatMoney(reservation.getTotal())));

        return receipt.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }

    private void validateReservation(Room room, LocalDate checkIn, LocalDate checkOut, int guests, Long excludeReservationId) {
        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room capacity exceeded. Max: " + room.getCapacity() + ", requested: " + guests);
        }

        List<Reservation> conflicts = reservationRepository.findConflictingReservations(
            room.getNumber(), checkIn, checkOut, excludeReservationId);

        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }
    }

    public List<Reservation> findByCheckInMonth(int year, int month) {
        return reservationRepository.findByCheckInMonth(year, month);
    }

    public List<Reservation> findOccupyingRoom(String roomNumber, LocalDate date) {
        return reservationRepository.findOccupyingRoom(roomNumber, date);
    }
}

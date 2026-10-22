package com.pousadamareaalta.service;

import com.pousadamareaalta.exception.*;
import com.pousadamareaalta.model.Reservation;
import com.pousadamareaalta.model.ReservationStatus;
import com.pousadamareaalta.model.Room;
import com.pousadamareaalta.pricing.PricingResult;
import com.pousadamareaalta.pricing.RatePlanFactory;
import com.pousadamareaalta.pricing.RatePlanStrategy;
import com.pousadamareaalta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private final MessageService messageService;

    public ReservationService(ReservationRepository reservationRepository, RoomService roomService, MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
        this.messageService = messageService;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests,
                                        com.pousadamareaalta.model.RatePlan ratePlan, String companyCode) {

        Room room = roomService.getRoom(roomNumber);

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room capacity exceeded");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out must be after check-in");
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);

        RatePlanStrategy strategy = RatePlanFactory.getStrategy(ratePlan);
        strategy.validate(checkIn, checkOut, nights, companyCode);

        List<Reservation> conflicts = reservationRepository.findConflicting(roomNumber, checkIn, checkOut, null);
        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException("Room is not available for the selected dates");
        }

        PricingResult pricing = strategy.calculatePrice(room.getType(), checkIn, checkOut, nights, companyCode);

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
        reservation.setNights(nights);
        reservation.setSubtotal(pricing.getSubtotal());
        reservation.setDiscount(pricing.getDiscount());
        reservation.setTotal(pricing.getTotal());
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setAmountPaid(BigDecimal.ZERO);
        reservation.setRefund(null);

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Only pending reservations can be confirmed");
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
            throw new OperationNotAllowedException("Cannot cancel reservation after check-in");
        }

        RatePlanStrategy strategy = RatePlanFactory.getStrategy(reservation.getRatePlan());
        BigDecimal refundAmount = strategy.calculateRefund(reservation.getAmountPaid());

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refundAmount);

        return reservationRepository.save(reservation);
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Only confirmed reservations can be checked in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);

        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Only checked-in reservations can be checked out");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);

        return reservationRepository.save(reservation);
    }

    private void sendConfirmationEmail(Reservation reservation) {
        String receipt = generateReceipt(reservation);
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

    public String generateReceipt(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Receipt is only available for paid reservations");
        }

        Room room = roomService.getRoom(reservation.getRoomNumber());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        StringBuilder receipt = new StringBuilder();
        receipt.append(String.format("Comprovante de reserva nº %d%n", reservation.getId()));
        receipt.append(String.format("Hóspede: %s (CPF %s)%n", reservation.getGuestName(), reservation.getGuestCpf()));
        receipt.append(String.format("Quarto: %d (%s)%n", reservation.getRoomNumber(), room.getType().name()));
        receipt.append(String.format("Entrada: %s | Saída: %s | %d %s%n",
            reservation.getCheckIn().format(formatter),
            reservation.getCheckOut().format(formatter),
            reservation.getNights(),
            reservation.getNights() == 1 ? "diária" : "diárias"));
        receipt.append(String.format("Diárias: R$ %s%n", formatMoney(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("Desconto: R$ %s%n", formatMoney(reservation.getDiscount())));
        }

        receipt.append(String.format("Total: R$ %s", formatMoney(reservation.getTotal())));

        return receipt.toString();
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("%,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }
}

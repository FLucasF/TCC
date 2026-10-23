package com.pousadamarealta.service;

import com.pousadamarealta.database.ReservationDao;
import com.pousadamarealta.database.RoomDao;
import com.pousadamarealta.domain.*;
import com.pousadamarealta.service.exceptions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReservationService {
    private final ReservationDao reservationDao;
    private final RoomDao roomDao;
    private final PricingCalculator pricingCalculator;
    private final MessageService messageService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ReservationService(ReservationDao reservationDao, RoomDao roomDao,
                            PricingCalculator pricingCalculator, MessageService messageService) {
        this.reservationDao = reservationDao;
        this.roomDao = roomDao;
        this.pricingCalculator = pricingCalculator;
        this.messageService = messageService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail, String guestCpf,
                                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {

        Room room = roomDao.findByNumber(roomNumber)
            .orElseThrow(() -> new RoomNotFoundException("Room " + roomNumber + " not found"));

        if (guests < 1) {
            throw new InvalidRequestException("At least one guest is required");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException("Room " + roomNumber + " has capacity for " +
                room.getCapacity() + " guests, but " + guests + " were requested");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Check-out date must be after check-in date");
        }

        if (reservationDao.hasConflict(roomNumber, checkIn, checkOut, null)) {
            throw new RoomUnavailableException("Room " + roomNumber + " is not available for the requested dates");
        }

        PricingCalculator.PricingResult pricing = pricingCalculator.calculate(
            room.getType(), checkIn, checkOut, ratePlan);

        Reservation reservation = new Reservation(
            null,
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            ratePlan,
            pricing.getNights(),
            pricing.getSubtotal(),
            pricing.getDiscount(),
            pricing.getTotal(),
            ReservationStatus.PENDING,
            BigDecimal.ZERO,
            null
        );

        return reservationDao.create(reservation);
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = reservationDao.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation " + id + " not found"));

        if (!reservation.canConfirm()) {
            throw new OperationNotAllowedException("Cannot confirm reservation in status " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationDao.update(reservation);

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = reservationDao.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation " + id + " not found"));

        if (!reservation.canCheckIn()) {
            throw new OperationNotAllowedException("Cannot check in reservation in status " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationDao.update(reservation);

        return reservation;
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = reservationDao.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation " + id + " not found"));

        if (!reservation.canCheckOut()) {
            throw new OperationNotAllowedException("Cannot check out reservation in status " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationDao.update(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = reservationDao.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation " + id + " not found"));

        if (!reservation.canCancel()) {
            throw new OperationNotAllowedException("Cannot cancel reservation in status " + reservation.getStatus());
        }

        BigDecimal refundAmount = calculateRefund(reservation);

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refundAmount);
        reservationDao.update(reservation);

        return reservation;
    }

    public Reservation getReservation(Long id) {
        return reservationDao.findById(id)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation " + id + " not found"));
    }

    private BigDecimal calculateRefund(Reservation reservation) {
        if (reservation.getRatePlan() == RatePlan.STANDARD) {
            return reservation.getAmountPaid();
        }
        return BigDecimal.ZERO;
    }

    private void sendConfirmationEmail(Reservation reservation) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationEmail(reservation);
        messageService.send(reservation.getGuestEmail(), subject, body);
    }

    private String buildConfirmationEmail(Reservation reservation) {
        String receipt = buildReceipt(reservation);

        return String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""",
            reservation.getGuestName(),
            receipt
        );
    }

    public String buildReceipt(Reservation reservation) {
        Room room = roomDao.findByNumber(reservation.getRoomNumber()).orElseThrow();

        String nightsText = reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias";

        StringBuilder receipt = new StringBuilder();
        receipt.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        receipt.append(String.format("Hóspede: %s (CPF %s)\n",
            reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));
        receipt.append(String.format("Quarto: %s (%s)\n",
            reservation.getRoomNumber(), room.getType().name()));
        receipt.append(String.format("Entrada: %s | Saída: %s | %s\n",
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER),
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
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }
}

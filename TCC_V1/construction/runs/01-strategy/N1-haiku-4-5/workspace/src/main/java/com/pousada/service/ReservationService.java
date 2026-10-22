package com.pousada.service;

import com.pousada.database.MessageRepository;
import com.pousada.database.ReservationRepository;
import com.pousada.database.RoomRepository;
import com.pousada.exception.ApiException;
import com.pousada.model.Reservation;
import com.pousada.model.ReservationStatus;
import com.pousada.model.RatePlan;
import com.pousada.model.Room;
import com.pousada.service.pricing.CorporatePricingStrategy;
import com.pousada.service.pricing.PricingResult;
import com.pousada.service.pricing.PricingStrategy;
import com.pousada.service.pricing.PricingStrategyFactory;
import com.pousada.util.Formatter;
import com.pousada.util.PriceCalculator;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

public class ReservationService {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final MessageRepository messageRepository;

    public ReservationService(RoomRepository roomRepository, ReservationRepository reservationRepository,
                             MessageRepository messageRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.messageRepository = messageRepository;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                       String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                       int guests, RatePlan ratePlan, String companyCode) throws SQLException {
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new ApiException.RoomNotFound());

        if (guests < 1) {
            throw new ApiException.InvalidRequest("Number of guests must be at least 1");
        }

        if (guests > room.getCapacity()) {
            throw new ApiException.CapacityExceeded();
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new ApiException.InvalidRequest("Checkout must be after check-in");
        }

        if (!reservationRepository.isRoomAvailable(roomNumber, checkIn, checkOut)) {
            throw new ApiException.RoomUnavailable();
        }

        validateRatePlan(ratePlan, companyCode, checkIn, checkOut);

        int nights = PriceCalculator.calculateNights(checkIn, checkOut);
        PricingStrategy strategy = PricingStrategyFactory.create(ratePlan, companyCode);
        PricingResult pricing = strategy.calculatePrice(room, checkIn, checkOut, nights);

        return reservationRepository.create(
            roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
            ratePlan, companyCode, nights, pricing.getSubtotal(), pricing.getDiscount(), pricing.getTotal()
        );
    }

    public Reservation confirmReservation(long reservationId, BigDecimal amountPaid) throws SQLException {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ApiException.ReservationNotFound());

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ApiException.OperationNotAllowed();
        }

        reservationRepository.updateStatus(reservationId, ReservationStatus.CONFIRMED);
        reservationRepository.updateAmountPaid(reservationId, amountPaid);

        Reservation confirmed = reservationRepository.findById(reservationId).orElseThrow();

        sendConfirmationEmail(confirmed);

        return confirmed;
    }

    public Reservation checkIn(long reservationId) throws SQLException {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ApiException.ReservationNotFound());

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ApiException.OperationNotAllowed();
        }

        reservationRepository.updateStatus(reservationId, ReservationStatus.CHECKED_IN);
        return reservationRepository.findById(reservationId).orElseThrow();
    }

    public Reservation checkOut(long reservationId) throws SQLException {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ApiException.ReservationNotFound());

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new ApiException.OperationNotAllowed();
        }

        reservationRepository.updateStatus(reservationId, ReservationStatus.CHECKED_OUT);
        return reservationRepository.findById(reservationId).orElseThrow();
    }

    public Reservation cancelReservation(long reservationId) throws SQLException {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ApiException.ReservationNotFound());

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ApiException.OperationNotAllowed();
        }

        PricingStrategy strategy = PricingStrategyFactory.create(
            reservation.getRatePlan(), reservation.getCompanyCode());
        BigDecimal refund = strategy.getRefundPolicy().calculateRefund(reservation.getAmountPaid());

        reservationRepository.updateStatus(reservationId, ReservationStatus.CANCELLED);
        reservationRepository.updateRefund(reservationId, refund);

        return reservationRepository.findById(reservationId).orElseThrow();
    }

    private void validateRatePlan(RatePlan ratePlan, String companyCode, LocalDate checkIn, LocalDate checkOut) {
        switch (ratePlan) {
            case CORPORATE:
                if (companyCode == null || !CorporatePricingStrategy.isValidCompanyCode(companyCode)) {
                    throw new ApiException.InvalidRequest("Invalid or missing company code");
                }
                break;
            case WEEKEND_PACKAGE:
                if (!PriceCalculator.isWeekendPackageValid(checkIn, checkOut)) {
                    throw new ApiException.InvalidRequest("Weekend package requires Friday check-in and Sunday check-out (2 nights)");
                }
                break;
            case LONG_STAY:
                int nights = PriceCalculator.calculateNights(checkIn, checkOut);
                if (nights < 7) {
                    throw new ApiException.InvalidRequest("Long stay requires at least 7 nights");
                }
                break;
        }
    }

    private void sendConfirmationEmail(Reservation reservation) throws SQLException {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        StringBuilder emailBody = new StringBuilder();
        emailBody.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        emailBody.append("Sua reserva na Pousada Maré Alta está confirmada. ");
        emailBody.append("Estes são os dados dela:\n\n");
        emailBody.append(formatReceipt(reservation, room)).append("\n\n");
        emailBody.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        emailBody.append("Até breve!");

        messageRepository.create(
            "EMAIL",
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            emailBody.toString()
        );
    }

    private String formatReceipt(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName())
          .append(" (CPF ").append(Formatter.formatCpf(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber())
          .append(" (").append(room.getType().getDisplayName()).append(")\n");
        sb.append("Entrada: ").append(Formatter.formatDateDisplay(reservation.getCheckIn()))
          .append(" | Saída: ").append(Formatter.formatDateDisplay(reservation.getCheckOut()))
          .append(" | ").append(Formatter.formatNights(reservation.getNights())).append("\n");
        sb.append("Diárias: ").append(Formatter.formatCurrency(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(Formatter.formatCurrency(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(Formatter.formatCurrency(reservation.getTotal()));
        return sb.toString();
    }
}

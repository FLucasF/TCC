package com.mareaalta.service;

import com.mareaalta.domain.Message;
import com.mareaalta.domain.Reservation;
import com.mareaalta.domain.Room;
import com.mareaalta.infrastructure.MessageRepository;
import com.mareaalta.infrastructure.ReservationRepository;
import com.mareaalta.infrastructure.RoomRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

public class ReservationService {
    public static Reservation createReservation(
            int roomNumber, String guestName, String guestEmail,
            String guestCpf, String companyCnpj, String companyName,
            String guestPassport, String guestCountry,
            LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan) throws SQLException {

        Room room = RoomRepository.findByNumber(roomNumber);
        if (room == null) {
            throw new ValidationException("ROOM_NOT_FOUND", "Quarto não encontrado");
        }

        if (!ReservationRepository.isRoomAvailable(roomNumber, checkIn, checkOut)) {
            throw new ValidationException("ROOM_UNAVAILABLE", "Quarto indisponível para as datas selecionadas");
        }

        if (guests < 1) {
            throw new ValidationException("INVALID_REQUEST", "A reserva deve ter pelo menos 1 hóspede");
        }

        if (guests > room.getCapacity()) {
            throw new ValidationException("CAPACITY_EXCEEDED", "Número de hóspedes excede a capacidade do quarto");
        }

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setGuestCpf(guestCpf);
        reservation.setCompanyCnpj(companyCnpj);
        reservation.setCompanyName(companyName);
        reservation.setGuestPassport(guestPassport);
        reservation.setGuestCountry(guestCountry);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(Reservation.RatePlan.valueOf(ratePlan));

        int nights = PricingService.countNights(checkIn, checkOut);
        reservation.setNights(nights);

        BigDecimal subtotal = PricingService.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        BigDecimal discount = PricingService.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PricingService.calculateTotal(subtotal, discount);

        reservation.setSubtotal(subtotal);
        reservation.setDiscount(discount);
        reservation.setTotal(total);
        reservation.setStatus(Reservation.Status.PENDING);
        reservation.setAmountPaid(BigDecimal.ZERO);

        return ReservationRepository.save(reservation);
    }

    public static Reservation getReservation(long id) throws SQLException {
        Reservation res = ReservationRepository.findById(id);
        if (res == null) {
            throw new ValidationException("RESERVATION_NOT_FOUND", "Reserva não encontrada");
        }
        return res;
    }

    public static Reservation confirmReservation(long id, BigDecimal amountPaid) throws SQLException {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != Reservation.Status.PENDING) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Operação não permitida para o estado atual da reserva");
        }

        reservation.setStatus(Reservation.Status.CONFIRMED);
        reservation.setAmountPaid(amountPaid);

        ReservationRepository.update(reservation);

        Room room = RoomRepository.findByNumber(reservation.getRoomNumber());
        String documentText = generateDocumentText(reservation, room);

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = DocumentGenerator.generateConfirmationEmail(reservation.getGuestName(), documentText);

        Message message = new Message("EMAIL", reservation.getGuestEmail(), subject, body);
        MessageRepository.save(message);

        return reservation;
    }

    public static Reservation checkIn(long id) throws SQLException {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != Reservation.Status.CONFIRMED) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Operação não permitida para o estado atual da reserva");
        }

        reservation.setStatus(Reservation.Status.CHECKED_IN);
        ReservationRepository.update(reservation);

        return reservation;
    }

    public static Reservation checkOut(long id) throws SQLException {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != Reservation.Status.CHECKED_IN) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Operação não permitida para o estado atual da reserva");
        }

        reservation.setStatus(Reservation.Status.CHECKED_OUT);
        ReservationRepository.update(reservation);

        return reservation;
    }

    public static Reservation cancelReservation(long id) throws SQLException {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != Reservation.Status.PENDING &&
            reservation.getStatus() != Reservation.Status.CONFIRMED) {
            throw new ValidationException("OPERATION_NOT_ALLOWED", "Operação não permitida para o estado atual da reserva");
        }

        BigDecimal refund = BigDecimal.ZERO;

        if (reservation.getStatus() == Reservation.Status.CONFIRMED &&
            reservation.getRatePlan() == Reservation.RatePlan.STANDARD) {
            refund = reservation.getAmountPaid();
        }

        reservation.setStatus(Reservation.Status.CANCELLED);
        reservation.setRefund(refund);

        ReservationRepository.update(reservation);

        return reservation;
    }

    public static String generateDocumentText(Reservation reservation, Room room) {
        if (reservation.getGuestCpf() != null) {
            return DocumentGenerator.generateComprovante(reservation, room);
        } else if (reservation.getCompanyCnpj() != null) {
            return DocumentGenerator.generateFatura(reservation, room);
        } else if (reservation.getGuestPassport() != null) {
            return DocumentGenerator.generateBookingReceipt(reservation, room);
        }
        return "";
    }

    public static class ValidationException extends RuntimeException {
        private final String code;

        public ValidationException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}

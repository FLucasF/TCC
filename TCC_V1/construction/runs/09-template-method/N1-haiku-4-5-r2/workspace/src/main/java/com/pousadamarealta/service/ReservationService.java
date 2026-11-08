package com.pousadamarealta.service;

import com.pousadamarealta.infrastructure.Database;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.util.PriceCalculator;
import com.pousadamarealta.util.Formatter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReservationService {
    private ReservationRepository reservationRepository;
    private RoomRepository roomRepository;
    private MessageRepository messageRepository;

    public ReservationService(Database database) {
        this.reservationRepository = new ReservationRepository(database);
        this.roomRepository = new RoomRepository(database);
        this.messageRepository = new MessageRepository(database);
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) throws Exception {
        Room room = roomRepository.findByNumber(roomNumber);
        if (room == null) {
            throw new ValidationException("ROOM_NOT_FOUND");
        }

        if (guests < 1 || guests > room.getCapacity()) {
            throw new ValidationException("CAPACITY_EXCEEDED");
        }

        List<Reservation> existingReservations = reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
        if (!existingReservations.isEmpty()) {
            throw new ValidationException("ROOM_UNAVAILABLE");
        }

        int nights = PriceCalculator.countNights(checkIn, checkOut);
        BigDecimal subtotal = PriceCalculator.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(roomNumber, guestName, guestEmail, guestCpf,
                                                   checkIn, checkOut, guests, ratePlan,
                                                   nights, subtotal, discount, total);
        return reservationRepository.create(reservation);
    }

    public Reservation getReservation(Long id) throws Exception {
        Reservation reservation = reservationRepository.findById(id);
        if (reservation == null) {
            throw new ValidationException("RESERVATION_NOT_FOUND");
        }
        return reservation;
    }

    public Reservation confirmReservation(Long id, BigDecimal amountPaid) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ValidationException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(amountPaid);
        reservationRepository.update(reservation);

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
            reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ValidationException("OPERATION_NOT_ALLOWED");
        }

        BigDecimal refund = BigDecimal.ZERO;
        if (reservation.getStatus() == ReservationStatus.CONFIRMED &&
            reservation.getRatePlan() == RatePlan.STANDARD) {
            refund = reservation.getAmountPaid();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refund);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkIn(Long id) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ValidationException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkOut(Long id) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new ValidationException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.update(reservation);

        return reservation;
    }

    private void sendConfirmationEmail(Reservation reservation) throws Exception {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber());
        String comprovante = buildComprovante(reservation, room);

        String emailBody = String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.getGuestName(),
            comprovante
        );

        messageRepository.create(new com.pousadamarealta.model.Message(
            "EMAIL",
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            emailBody
        ));
    }

    public String buildComprovante(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append(" (CPF ").append(Formatter.formatCpf(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(room.getType()).append(")\n");
        sb.append("Entrada: ").append(Formatter.formatBrDate(reservation.getCheckIn())).append(" | ");
        sb.append("Saída: ").append(Formatter.formatBrDate(reservation.getCheckOut())).append(" | ");
        sb.append(reservation.getNights()).append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(Formatter.formatCurrency(reservation.getSubtotal())).append("\n");
        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(Formatter.formatCurrency(reservation.getDiscount())).append("\n");
        }
        sb.append("Total: ").append(Formatter.formatCurrency(reservation.getTotal()));
        return sb.toString();
    }
}

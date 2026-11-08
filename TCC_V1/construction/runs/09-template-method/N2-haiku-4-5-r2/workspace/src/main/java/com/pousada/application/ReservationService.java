package com.pousada.application;

import com.pousada.domain.*;
import com.pousada.infrastructure.MessageRepository;
import com.pousada.infrastructure.ReservationRepository;
import com.pousada.infrastructure.RoomRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReservationService {
    private final ReservationRepository reservationRepository = new ReservationRepository();
    private final RoomRepository roomRepository = new RoomRepository();
    private final MessageRepository messageRepository = new MessageRepository();

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                         String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                         int guests, RatePlan ratePlan) throws Exception {
        // Validate room exists
        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new ApiException("ROOM_NOT_FOUND", "Room not found", 404));

        // Validate check-out is after check-in
        if (!checkOut.isAfter(checkIn)) {
            throw new ApiException("INVALID_REQUEST", "Check-out must be after check-in", 400);
        }

        // Validate guests count
        if (guests < 1) {
            throw new ApiException("INVALID_REQUEST", "Must have at least 1 guest", 400);
        }

        if (guests > room.getCapacity()) {
            throw new ApiException("CAPACITY_EXCEEDED", "Room capacity exceeded", 422);
        }

        // Check for room availability
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            var conflicts = reservationRepository.findByRoomAndNight(roomNumber, current);
            if (!conflicts.isEmpty()) {
                throw new ApiException("ROOM_UNAVAILABLE", "Room is unavailable for these dates", 409);
            }
            current = current.plusDays(1);
        }

        // Calculate pricing
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        double subtotal = SeasonCalculator.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        double discount = Rounding.round(subtotal * ratePlan.getDiscountPercentage());
        double total = Rounding.round(subtotal - discount);

        // Create reservation
        Reservation reservation = new Reservation(
            roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan,
            nights, subtotal, discount, total
        );

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(long reservationId) throws Exception {
        return reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404));
    }

    public Reservation confirmReservation(long reservationId, double amountPaid) throws Exception {
        Reservation reservation = getReservation(reservationId);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Only pending reservations can be confirmed", 409);
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(amountPaid);
        reservationRepository.update(reservation);

        // Send confirmation email
        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation checkInReservation(long reservationId) throws Exception {
        Reservation reservation = getReservation(reservationId);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Only confirmed reservations can check in", 409);
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkOutReservation(long reservationId) throws Exception {
        Reservation reservation = getReservation(reservationId);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Only checked-in reservations can check out", 409);
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation cancelReservation(long reservationId) throws Exception {
        Reservation reservation = getReservation(reservationId);

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Only pending or confirmed reservations can be cancelled", 409);
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        // Calculate refund
        if (reservation.getRatePlan() == RatePlan.STANDARD) {
            reservation.setRefund(reservation.getAmountPaid());
        } else {
            reservation.setRefund(0.0);
        }

        reservationRepository.update(reservation);

        return reservation;
    }

    private void sendConfirmationEmail(Reservation reservation) throws Exception {
        String receipt = generateReceipt(reservation);
        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """, reservation.getGuestName(), receipt);

        Message message = new Message(
            MessageChannel.EMAIL,
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );

        messageRepository.save(message);
    }

    private String generateReceipt(Reservation reservation) throws Exception {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
            .orElseThrow();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        String nightsText = reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias";

        StringBuilder receipt = new StringBuilder();
        receipt.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        receipt.append(String.format("Hóspede: %s (CPF %s)\n", reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));
        receipt.append(String.format("Quarto: %d (%s)\n", reservation.getRoomNumber(), room.getType().getDisplayName()));
        receipt.append(String.format("Entrada: %s | Saída: %s | %s\n",
            reservation.getCheckIn().format(dateFormatter),
            reservation.getCheckOut().format(dateFormatter),
            nightsText));
        receipt.append(String.format("Diárias: R$ %s\n", formatMoney(reservation.getSubtotal())));

        if (reservation.getDiscount() > 0) {
            receipt.append(String.format("Desconto: R$ %s\n", formatMoney(reservation.getDiscount())));
        }

        receipt.append(String.format("Total: R$ %s", formatMoney(reservation.getTotal())));

        return receipt.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() != 11) {
            return cpf;
        }
        return String.format("%s.%s.%s-%s",
            cpf.substring(0, 3),
            cpf.substring(3, 6),
            cpf.substring(6, 9),
            cpf.substring(9, 11));
    }

    private String formatMoney(double value) {
        String formatted = String.format("%.2f", value);
        return formatted.replace(".", ",");
    }

    public String getReceiptDocument(long reservationId) throws Exception {
        Reservation reservation = getReservation(reservationId);

        if (reservation.getStatus() == ReservationStatus.PENDING || reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ApiException("OPERATION_NOT_ALLOWED", "Receipt is only available for paid reservations", 409);
        }

        return generateReceipt(reservation);
    }
}

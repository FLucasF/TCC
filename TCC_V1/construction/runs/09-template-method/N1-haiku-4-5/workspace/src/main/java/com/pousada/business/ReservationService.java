package com.pousada.business;

import com.pousada.db.*;
import com.pousada.models.*;
import com.pousada.utils.PricingUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepo;
    private final RoomRepository roomRepo;
    private final MessageRepository messageRepo;

    public ReservationService(ReservationRepository reservationRepo, RoomRepository roomRepo, MessageRepository messageRepo) {
        this.reservationRepo = reservationRepo;
        this.roomRepo = roomRepo;
        this.messageRepo = messageRepo;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) throws Exception {
        Optional<Room> room = roomRepo.findByNumber(roomNumber);
        if (room.isEmpty()) {
            throw new ApiException(404, "ROOM_NOT_FOUND", "Room not found");
        }

        if (guests < 1) {
            throw new ApiException(400, "INVALID_REQUEST", "At least one guest is required");
        }

        if (guests > room.get().getCapacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED", "Too many guests for this room");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new ApiException(400, "INVALID_REQUEST", "Check-out must be after check-in");
        }

        List<Reservation> conflicts = reservationRepo.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
        if (!conflicts.isEmpty()) {
            throw new ApiException(409, "ROOM_UNAVAILABLE", "Room is not available for these dates");
        }

        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal[] nightlyPrices = new BigDecimal[nights];
        for (int i = 0; i < nights; i++) {
            LocalDate night = checkIn.plusDays(i);
            nightlyPrices[i] = PricingUtils.calculateNightlyPrice(room.get().getNightlyRate(), night);
        }

        BigDecimal subtotal = PricingUtils.calculateSubtotal(nightlyPrices);
        BigDecimal discount = PricingUtils.calculateDiscount(subtotal, ratePlan == RatePlan.NON_REFUNDABLE);
        BigDecimal total = PricingUtils.calculateTotal(subtotal, discount);

        return reservationRepo.create(roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut,
                guests, ratePlan, nights, subtotal, discount, total);
    }

    public Reservation confirmReservation(long reservationId, BigDecimal amountPaid) throws Exception {
        Optional<Reservation> reservation = reservationRepo.findById(reservationId);
        if (reservation.isEmpty()) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
        }

        Reservation res = reservation.get();
        if (res.getStatus() != ReservationStatus.PENDING) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Cannot confirm a reservation that is not pending");
        }

        Reservation confirmed = res.withStatus(ReservationStatus.CONFIRMED)
                .withAmountPaid(amountPaid);
        reservationRepo.update(confirmed);

        sendConfirmationEmail(confirmed);

        return confirmed;
    }

    public Reservation cancelReservation(long reservationId) throws Exception {
        Optional<Reservation> reservation = reservationRepo.findById(reservationId);
        if (reservation.isEmpty()) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
        }

        Reservation res = reservation.get();
        if (res.getStatus() == ReservationStatus.CHECKED_OUT || res.getStatus() == ReservationStatus.CANCELLED) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Cannot cancel a reservation in this state");
        }

        if (res.getStatus() == ReservationStatus.CHECKED_IN) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Cannot cancel a checked-in reservation");
        }

        BigDecimal refund = BigDecimal.ZERO;
        if (res.getStatus() == ReservationStatus.CONFIRMED && res.getRatePlan() == RatePlan.STANDARD) {
            refund = res.getAmountPaid();
        }

        Reservation cancelled = res.withStatus(ReservationStatus.CANCELLED)
                .withRefund(refund);
        reservationRepo.update(cancelled);

        return cancelled;
    }

    public Reservation checkIn(long reservationId) throws Exception {
        Optional<Reservation> reservation = reservationRepo.findById(reservationId);
        if (reservation.isEmpty()) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
        }

        Reservation res = reservation.get();
        if (res.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Cannot check-in a reservation that is not confirmed");
        }

        Reservation checkedIn = res.withStatus(ReservationStatus.CHECKED_IN);
        reservationRepo.update(checkedIn);

        return checkedIn;
    }

    public Reservation checkOut(long reservationId) throws Exception {
        Optional<Reservation> reservation = reservationRepo.findById(reservationId);
        if (reservation.isEmpty()) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
        }

        Reservation res = reservation.get();
        if (res.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Cannot check-out a reservation that is not checked-in");
        }

        Reservation checkedOut = res.withStatus(ReservationStatus.CHECKED_OUT);
        reservationRepo.update(checkedOut);

        return checkedOut;
    }

    private void sendConfirmationEmail(Reservation reservation) throws Exception {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String comprovante = generateComprovante(reservation);
        String body = """
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """.formatted(reservation.getGuestName(), comprovante);

        messageRepo.create("EMAIL", reservation.getGuestEmail(), subject, body);
    }

    public String generateComprovante(Reservation reservation) throws Exception {
        Optional<Room> room = roomRepo.findByNumber(reservation.getRoomNumber());
        if (room.isEmpty()) {
            throw new Exception("Room not found");
        }

        String nightsText = reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias";
        String discountLine = reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0
                ? "Desconto: " + PricingUtils.formatCurrency(reservation.getDiscount()) + "\n"
                : "";

        return String.format("""
                Comprovante de reserva nº %d
                Hóspede: %s (CPF %s)
                Quarto: %d (%s)
                Entrada: %s | Saída: %s | %s
                Diárias: %s
                %sTotal: %s""",
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestCpf(),
                reservation.getRoomNumber(),
                room.get().getType().getLabel(),
                PricingUtils.formatDate(reservation.getCheckIn()),
                PricingUtils.formatDate(reservation.getCheckOut()),
                nightsText,
                PricingUtils.formatCurrency(reservation.getSubtotal()),
                discountLine,
                PricingUtils.formatCurrency(reservation.getTotal())
        );
    }
}

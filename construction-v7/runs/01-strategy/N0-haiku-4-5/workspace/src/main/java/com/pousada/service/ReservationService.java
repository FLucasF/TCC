package com.pousada.service;

import com.pousada.domain.*;
import com.pousada.exception.ApiException;
import com.pousada.service.pricing.PricingStrategy;
import com.pousada.service.pricing.PricingStrategyFactory;
import com.pousada.util.MoneyRounder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {
    private final List<Reservation> reservations = new ArrayList<>();
    private final RoomService roomService;
    private final MessageService messageService;
    private long nextId = 1;

    public ReservationService(RoomService roomService, MessageService messageService) {
        this.roomService = roomService;
        this.messageService = messageService;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                       String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                       int guests, RatePlan ratePlan, String companyCode) throws ApiException {
        if (guests < 1) {
            throw new ApiException(400, "INVALID_REQUEST", "At least one guest is required");
        }

        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            throw new ApiException(400, "INVALID_REQUEST", "Check-out must be after check-in");
        }

        Room room = roomService.getRoomByNumber(roomNumber);

        if (guests > room.getCapacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED", "Number of guests exceeds room capacity");
        }

        checkRoomAvailability(roomNumber, checkIn, checkOut);

        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

        PricingStrategy pricingStrategy = PricingStrategyFactory.getStrategy(ratePlan);
        PricingStrategy.PricingResult pricingResult = pricingStrategy.calculatePrice(room, checkIn, checkOut, nights, companyCode);

        Reservation reservation = new Reservation(
                nextId++,
                roomNumber,
                guestName,
                guestEmail,
                guestCpf,
                checkIn,
                checkOut,
                guests,
                ratePlan,
                companyCode,
                nights,
                pricingResult.subtotal,
                pricingResult.discount,
                pricingResult.total
        );

        reservations.add(reservation);
        return reservation;
    }

    public Reservation getReservation(long id) throws ApiException {
        return reservations.stream()
                .filter(r -> r.getId() == id)
                .findFirst()
                .orElseThrow(() -> new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found"));
    }

    public Reservation confirmReservation(long id) throws ApiException {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Only pending reservations can be confirmed");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation checkIn(long id) throws ApiException {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Only confirmed reservations can check in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);

        return reservation;
    }

    public ReservationDocument checkOut(long id) throws ApiException {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Only checked-in reservations can check out");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);

        return createDocument(reservation);
    }

    public Reservation cancelReservation(long id) throws ApiException {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
            reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Reservation cannot be cancelled in this status");
        }

        PricingStrategy pricingStrategy = PricingStrategyFactory.getStrategy(reservation.getRatePlan());
        BigDecimal refundPercentage = BigDecimal.ZERO;

        try {
            Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
            PricingStrategy.PricingResult pricingResult = pricingStrategy.calculatePrice(
                    room, reservation.getCheckIn(), reservation.getCheckOut(),
                    reservation.getNights(), reservation.getCompanyCode());
            refundPercentage = pricingResult.refundPercentage;
        } catch (ApiException e) {
            refundPercentage = BigDecimal.ZERO;
        }

        BigDecimal refund = MoneyRounder.multiply(
                reservation.getAmountPaid(),
                refundPercentage.divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_EVEN)
        );

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refund);

        return reservation;
    }

    public ReservationDocument getDocument(long id) throws ApiException {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED &&
            reservation.getStatus() != ReservationStatus.CHECKED_IN &&
            reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            throw new ApiException(400, "INVALID_REQUEST", "Document only available for paid reservations");
        }

        return createDocument(reservation);
    }

    private void checkRoomAvailability(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws ApiException {
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            final LocalDate night = current;
            boolean occupied = reservations.stream()
                    .filter(r -> r.getRoomNumber() == roomNumber)
                    .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                    .anyMatch(r -> !night.isBefore(r.getCheckIn()) && night.isBefore(r.getCheckOut()));

            if (occupied) {
                throw new ApiException(409, "ROOM_UNAVAILABLE", "Room is not available for the selected dates");
            }

            current = current.plusDays(1);
        }
    }

    private void sendConfirmationEmail(Reservation reservation) {
        Room room = null;
        try {
            room = roomService.getRoomByNumber(reservation.getRoomNumber());
        } catch (ApiException e) {
            return;
        }

        ReservationDocument doc = new ReservationDocument(
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

        String documentText = formatDocument(doc);

        String body = "Olá, " + reservation.getGuestName() + "!\n\n" +
                "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                documentText +
                "\n\nA entrada é a partir das 14h, e a saída, até as 12h.\n" +
                "Até breve!";

        messageService.sendEmail(
                reservation.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta",
                body
        );
    }

    private ReservationDocument createDocument(Reservation reservation) {
        return new ReservationDocument(
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestCpf(),
                reservation.getRoomNumber(),
                getRoomType(reservation.getRoomNumber()),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal()
        );
    }

    private String formatDocument(ReservationDocument doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(doc.getReservationId()).append("\n");
        sb.append("Hóspede: ").append(doc.getGuestName()).append(" (CPF ").append(formatCpf(doc.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(doc.getRoomNumber()).append(" (").append(doc.getRoomType()).append(")\n");
        sb.append("Entrada: ").append(formatDate(doc.getCheckIn())).append(" | ");
        sb.append("Saída: ").append(formatDate(doc.getCheckOut())).append(" | ");
        sb.append(doc.getNights()).append(doc.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatMoney(doc.getSubtotal())).append("\n");
        if (doc.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(doc.getDiscount())).append("\n");
        }
        sb.append("Total: ").append(formatMoney(doc.getTotal()));
        return sb.toString();
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatMoney(BigDecimal value) {
        return String.format("R$ %.2f", value);
    }

    private String formatCpf(String cpf) {
        if (cpf.length() != 11) return cpf;
        return String.format("%s.%s.%s-%s",
                cpf.substring(0, 3),
                cpf.substring(3, 6),
                cpf.substring(6, 9),
                cpf.substring(9));
    }

    private String getRoomType(int roomNumber) {
        try {
            return roomService.getRoomByNumber(roomNumber).getType();
        } catch (ApiException e) {
            return "UNKNOWN";
        }
    }

    public List<Reservation> getAllReservations() {
        return new ArrayList<>(reservations);
    }
}

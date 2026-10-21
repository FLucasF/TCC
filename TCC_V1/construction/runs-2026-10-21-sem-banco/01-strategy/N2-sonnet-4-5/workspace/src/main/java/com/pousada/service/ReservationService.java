package com.pousada.service;

import com.pousada.domain.*;
import com.pousada.exception.BusinessException;
import com.pousada.rateplan.PricingContext;
import com.pousada.rateplan.PricingResult;
import com.pousada.rateplan.RatePlanRegistry;
import com.pousada.rateplan.RatePlanStrategy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationService {

    private final List<Reservation> reservations = new ArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final RoomService roomService;
    private final MessageService messageService;

    public ReservationService(RoomService roomService, MessageService messageService) {
        this.roomService = roomService;
        this.messageService = messageService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan, String companyCode) {
        Room room = roomService.getRoom(roomNumber);

        if (guests < 1) {
            throw new BusinessException("INVALID_REQUEST", "At least one guest is required", 400);
        }

        if (guests > room.getCapacity()) {
            throw new BusinessException("CAPACITY_EXCEEDED",
                "Room capacity exceeded. Maximum: " + room.getCapacity(), 422);
        }

        int nights = (int) checkIn.until(checkOut, java.time.temporal.ChronoUnit.DAYS);

        PricingContext context = new PricingContext(room.getType(), checkIn, checkOut,
                                                    guests, companyCode);

        RatePlanStrategy strategy = RatePlanRegistry.getStrategy(ratePlan);
        strategy.validate(context);

        PricingResult pricing = strategy.calculatePrice(context);

        checkRoomAvailability(roomNumber, checkIn, checkOut, null);

        Reservation reservation = new Reservation(
            nextId.getAndIncrement(),
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
            pricing.getSubtotal(),
            pricing.getDiscount(),
            pricing.getTotal()
        );

        reservations.add(reservation);
        return reservation;
    }

    public Reservation getReservation(long id) {
        return reservations.stream()
            .filter(r -> r.getId() == id)
            .findFirst()
            .orElseThrow(() -> new BusinessException("RESERVATION_NOT_FOUND",
                "Reservation not found: " + id, 404));
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException("OPERATION_NOT_ALLOWED",
                "Can only confirm pending reservations", 409);
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException("OPERATION_NOT_ALLOWED",
                "Can only check-in confirmed reservations", 409);
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservation;
    }

    public Reservation checkOut(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new BusinessException("OPERATION_NOT_ALLOWED",
                "Can only check-out checked-in reservations", 409);
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException("OPERATION_NOT_ALLOWED",
                "Can only cancel pending or confirmed reservations", 409);
        }

        RatePlanStrategy strategy = RatePlanRegistry.getStrategy(reservation.getRatePlan());
        BigDecimal refund = strategy.calculateRefund(reservation.getAmountPaid());

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refund);

        return reservation;
    }

    public String generateDocument(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.PENDING) {
            throw new BusinessException("OPERATION_NOT_ALLOWED",
                "Document only available for paid reservations", 409);
        }

        return formatDocument(reservation);
    }

    private void checkRoomAvailability(String roomNumber, LocalDate checkIn, LocalDate checkOut,
                                      Long excludeReservationId) {
        boolean hasConflict = reservations.stream()
            .filter(r -> r.getRoomNumber().equals(roomNumber))
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
            .filter(r -> excludeReservationId == null || r.getId() != excludeReservationId)
            .anyMatch(r -> datesOverlap(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut));

        if (hasConflict) {
            throw new BusinessException("ROOM_UNAVAILABLE",
                "Room not available for the selected dates", 409);
        }
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private void sendConfirmationEmail(Reservation reservation) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.getGuestName(),
            formatDocument(reservation)
        );

        messageService.sendEmail(reservation.getGuestEmail(), subject, body);
    }

    private String formatDocument(Reservation reservation) {
        Room room = roomService.getRoom(reservation.getRoomNumber());
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        StringBuilder doc = new StringBuilder();
        doc.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        doc.append("Hóspede: ").append(reservation.getGuestName())
           .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        doc.append("Quarto: ").append(reservation.getRoomNumber())
           .append(" (").append(room.getType()).append(")\n");
        doc.append("Entrada: ").append(reservation.getCheckIn().format(dateFormatter))
           .append(" | Saída: ").append(reservation.getCheckOut().format(dateFormatter))
           .append(" | ").append(reservation.getNights())
           .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        doc.append("Diárias: ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            doc.append("Desconto: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        doc.append("Total: ").append(formatMoney(reservation.getTotal()));

        return doc.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X")
                                                 .replace(".", ",")
                                                 .replace("X", ".");
    }

    public List<Reservation> getAllReservations() {
        return new ArrayList<>(reservations);
    }
}

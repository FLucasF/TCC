package com.pousadamarealta.service;

import com.pousadamarealta.exception.*;
import com.pousadamarealta.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationService {
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final RoomService roomService;
    private final MessageService messageService;
    private final Map<RatePlan, RatePlanStrategy> strategies = new EnumMap<>(RatePlan.class);

    public ReservationService(RoomService roomService, MessageService messageService) {
        this.roomService = roomService;
        this.messageService = messageService;

        strategies.put(RatePlan.STANDARD, new StandardRatePlan());
        strategies.put(RatePlan.NON_REFUNDABLE, new NonRefundableRatePlan());
        strategies.put(RatePlan.CORPORATE, new CorporateRatePlan());
        strategies.put(RatePlan.WEEKEND_PACKAGE, new WeekendPackageRatePlan());
        strategies.put(RatePlan.LONG_STAY, new LongStayRatePlan());
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan, String companyCode) {

        Room room = roomService.getRoom(roomNumber);

        if (guests < 1) {
            throw new InvalidRequestException("O número de hóspedes deve ser no mínimo 1");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("A data de saída deve ser posterior à data de entrada");
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);

        RatePlanStrategy strategy = strategies.get(ratePlan);
        strategy.validate(checkIn, checkOut, nights, companyCode);

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new RoomUnavailableException();
        }

        PricingResult pricing;
        if (ratePlan == RatePlan.CORPORATE) {
            pricing = ((CorporateRatePlan) strategy).calculatePrice(room, checkIn, checkOut, nights, companyCode);
        } else {
            pricing = strategy.calculatePrice(room, checkIn, checkOut, nights);
        }

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
            pricing.subtotal(),
            pricing.discount(),
            pricing.total()
        );

        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Reservation getReservation(long id) {
        Reservation reservation = reservations.get(id);
        if (reservation == null) {
            throw new ReservationNotFoundException();
        }
        return reservation;
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException("Apenas reservas pendentes podem ser confirmadas");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Apenas reservas pendentes ou confirmadas podem ser canceladas");
        }

        RatePlanStrategy strategy = strategies.get(reservation.getRatePlan());
        BigDecimal refundAmount = strategy.calculateRefund(reservation.getAmountPaid());

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refundAmount);

        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException("Apenas reservas confirmadas podem fazer check-in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservation;
    }

    public Reservation checkOut(long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException("Apenas reservas com check-in podem fazer check-out");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return reservation;
    }

    public List<Reservation> getAllReservations() {
        return new ArrayList<>(reservations.values());
    }

    private boolean isRoomUnavailable(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return reservations.values().stream()
            .filter(r -> r.getRoomNumber().equals(roomNumber))
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
            .anyMatch(r -> datesOverlap(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut));
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private void sendConfirmationEmail(Reservation reservation) {
        Room room = roomService.getRoom(reservation.getRoomNumber());
        String receipt = formatReceipt(reservation, room);

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.getGuestName(),
            receipt
        );

        messageService.sendEmail(reservation.getGuestEmail(), subject, body);
    }

    private String formatReceipt(Reservation reservation, Room room) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        receipt.append("Hóspede: ").append(reservation.getGuestName())
               .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        receipt.append("Quarto: ").append(reservation.getRoomNumber())
               .append(" (").append(room.getType()).append(")\n");
        receipt.append("Entrada: ").append(formatDate(reservation.getCheckIn()))
               .append(" | Saída: ").append(formatDate(reservation.getCheckOut()))
               .append(" | ").append(reservation.getNights())
               .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        receipt.append("Diárias: ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append("Desconto: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        receipt.append("Total: ").append(formatMoney(reservation.getTotal()));

        return receipt.toString();
    }

    public String getReceiptText(Reservation reservation) {
        Room room = roomService.getRoom(reservation.getRoomNumber());
        return formatReceipt(reservation, room);
    }

    private String formatMoney(BigDecimal value) {
        return String.format("R$ %,.2f", value).replace(",", ".");
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9, 11);
        }
        return cpf;
    }
}

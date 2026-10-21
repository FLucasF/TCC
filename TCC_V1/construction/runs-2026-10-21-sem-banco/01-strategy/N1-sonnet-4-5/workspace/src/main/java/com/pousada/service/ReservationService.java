package com.pousada.service;

import com.pousada.exception.*;
import com.pousada.model.*;
import com.pousada.rateplan.RatePlan;
import com.pousada.rateplan.RatePlanFactory;
import com.pousada.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReservationService {
    private final RoomService roomService;
    private final MessageService messageService;
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private long nextId = 1;

    public ReservationService(RoomService roomService, MessageService messageService) {
        this.roomService = roomService;
        this.messageService = messageService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail, String guestCpf,
                                          LocalDate checkIn, LocalDate checkOut, int guests,
                                          RatePlanType ratePlanType, String companyCode) {
        Room room = roomService.getRoom(roomNumber);

        if (guests < 1) {
            throw new InvalidRequestException("Número de hóspedes deve ser no mínimo 1");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        if (!checkIn.isBefore(checkOut)) {
            throw new InvalidRequestException("Data de saída deve ser após a data de entrada");
        }

        RatePlan ratePlan = RatePlanFactory.create(ratePlanType);
        ratePlan.validate(room, checkIn, checkOut, guests, companyCode);

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new RoomUnavailableException();
        }

        BigDecimal subtotal = ratePlan.calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = ratePlan.calculateDiscount(subtotal);
        BigDecimal total = MoneyUtil.round(subtotal.subtract(discount));

        Reservation reservation = new Reservation(
            nextId++, roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlanType, subtotal, discount, total
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

        RatePlan ratePlan = RatePlanFactory.create(reservation.getRatePlan());
        BigDecimal refund = ratePlan.calculateRefund(reservation.getAmountPaid());

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refund);

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
            .filter(r -> !r.isCancelled())
            .anyMatch(r -> datesOverlap(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut));
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private void sendConfirmationEmail(Reservation reservation) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String document = generateDocumentText(reservation);

        String body = String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.getGuestName(),
            document
        );

        messageService.sendEmail(reservation.getGuestEmail(), subject, body);
    }

    public String generateDocumentText(Reservation reservation) {
        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException("Comprovante disponível apenas para reservas pagas");
        }

        Room room = roomService.getRoom(reservation.getRoomNumber());
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        StringBuilder doc = new StringBuilder();
        doc.append(String.format("Comprovante de reserva nº %d%n", reservation.getId()));
        doc.append(String.format("Hóspede: %s (CPF %s)%n", reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));
        doc.append(String.format("Quarto: %s (%s)%n", reservation.getRoomNumber(), room.getType()));
        doc.append(String.format("Entrada: %s | Saída: %s | %d %s%n",
            reservation.getCheckIn().format(dateFormatter),
            reservation.getCheckOut().format(dateFormatter),
            reservation.getNights(),
            reservation.getNights() == 1 ? "diária" : "diárias"));
        doc.append(String.format("Diárias: %s%n", formatMoney(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            doc.append(String.format("Desconto: %s%n", formatMoney(reservation.getDiscount())));
        }

        doc.append(String.format("Total: %s", formatMoney(reservation.getTotal())));

        return doc.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return String.format("%s.%s.%s-%s",
                cpf.substring(0, 3),
                cpf.substring(3, 6),
                cpf.substring(6, 9),
                cpf.substring(9, 11));
        }
        return cpf;
    }

    private String formatMoney(BigDecimal value) {
        return String.format("R$ %,.2f", value).replace(",", "X").replace(".", ",").replace("X", ".");
    }
}

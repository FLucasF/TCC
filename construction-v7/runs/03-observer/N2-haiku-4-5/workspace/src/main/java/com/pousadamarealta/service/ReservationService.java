package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepo;
    private final RoomRepository roomRepo;
    private final MessageRepository messageRepo;
    private final PricingService pricingService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter CSV_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String CURRENCY_FORMAT = "R$ %.2f";

    public ReservationService(
        ReservationRepository reservationRepo,
        RoomRepository roomRepo,
        MessageRepository messageRepo,
        PricingService pricingService
    ) {
        this.reservationRepo = reservationRepo;
        this.roomRepo = roomRepo;
        this.messageRepo = messageRepo;
        this.pricingService = pricingService;
    }

    public Reservation createReservation(
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan
    ) {
        Room room = roomRepo.findByNumber(roomNumber)
            .orElseThrow(() -> new IllegalArgumentException("ROOM_NOT_FOUND"));

        if (guests < 1 || guests > room.capacity()) {
            throw new IllegalArgumentException(guests < 1 ? "INVALID_REQUEST" : "CAPACITY_EXCEEDED");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        int nights = pricingService.countNights(checkIn, checkOut);
        BigDecimal subtotal = pricingService.calculateSubtotal(room.nightly(), checkIn, checkOut);
        BigDecimal discount = pricingService.calculateDiscount(
            subtotal,
            ratePlan == RatePlan.NON_REFUNDABLE
        );
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        long id = reservationRepo.getNextId();
        Reservation reservation = new Reservation(
            id,
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            ratePlan,
            nights,
            subtotal,
            discount,
            total
        );

        return reservationRepo.save(reservation);
    }

    public Reservation confirmReservation(long reservationId) {
        Reservation reservation = reservationRepo.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.confirm(reservation.getTotal());

        Room room = roomRepo.findByNumber(reservation.getRoomNumber()).orElseThrow();

        sendGuestConfirmationEmail(reservation, room);
        sendGovernanceConfirmationEmail(reservation, room);
        sendFinanceConfirmationEmail(reservation);

        return reservationRepo.save(reservation);
    }

    public Reservation cancelReservation(long reservationId) {
        Reservation reservation = reservationRepo.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
            reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        BigDecimal refund = BigDecimal.ZERO;
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            if (reservation.getRatePlan() == RatePlan.STANDARD) {
                refund = reservation.getAmountPaid();
            }
        }

        reservation.cancel(refund);

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            Room room = roomRepo.findByNumber(reservation.getRoomNumber()).orElseThrow();
            sendGovernanceCancellationEmail(reservation, room);
            if (refund.compareTo(BigDecimal.ZERO) > 0) {
                sendFinanceCancellationEmail(reservation);
            }
        }

        return reservationRepo.save(reservation);
    }

    public Reservation checkIn(long reservationId) {
        Reservation reservation = reservationRepo.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.checkIn();
        return reservationRepo.save(reservation);
    }

    public Reservation checkOut(long reservationId) {
        Reservation reservation = reservationRepo.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.checkOut();
        return reservationRepo.save(reservation);
    }

    public Reservation getReservation(long reservationId) {
        return reservationRepo.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));
    }

    public ReservationDocument getReservationDocument(long reservationId) {
        Reservation reservation = reservationRepo.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (reservation.getAmountPaid().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        Room room = roomRepo.findByNumber(reservation.getRoomNumber()).orElseThrow();
        return new ReservationDocument(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            room.type().displayName,
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return reservationRepo.findAll().stream()
            .filter(r -> r.getRoomNumber() == roomNumber)
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
            .anyMatch(r -> hasOverlap(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut));
    }

    private boolean hasOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private void sendGuestConfirmationEmail(Reservation reservation, Room room) {
        String body = buildGuestConfirmationBody(reservation, room);
        Message message = new Message(
            messageRepo.getNextId(),
            MessageChannel.EMAIL,
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );
        messageRepo.save(message);
    }

    private void sendGovernanceConfirmationEmail(Reservation reservation, Room room) {
        String dates = formatDateRange(reservation.getCheckIn(), reservation.getCheckOut());
        String subject = String.format("Reserva confirmada - quarto %d", reservation.getRoomNumber());
        String body = String.format(
            "Entrada: %s | Saída: %s\nHóspedes: %d",
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER),
            reservation.getGuests()
        );
        Message message = new Message(
            messageRepo.getNextId(),
            MessageChannel.EMAIL,
            "governanca@pousadamarealta.com.br",
            subject,
            body
        );
        messageRepo.save(message);
    }

    private void sendFinanceConfirmationEmail(Reservation reservation) {
        String subject = String.format("Pagamento recebido - reserva %d", reservation.getId());
        String body = String.format("Valor pago: %s", formatCurrency(reservation.getAmountPaid()));
        Message message = new Message(
            messageRepo.getNextId(),
            MessageChannel.EMAIL,
            "financeiro@pousadamarealta.com.br",
            subject,
            body
        );
        messageRepo.save(message);
    }

    private void sendGovernanceCancellationEmail(Reservation reservation, Room room) {
        String subject = String.format("Reserva cancelada - quarto %d", reservation.getRoomNumber());
        String body = String.format(
            "Datas liberadas: %s a %s",
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER)
        );
        Message message = new Message(
            messageRepo.getNextId(),
            MessageChannel.EMAIL,
            "governanca@pousadamarealta.com.br",
            subject,
            body
        );
        messageRepo.save(message);
    }

    private void sendFinanceCancellationEmail(Reservation reservation) {
        String subject = String.format("Reembolso - reserva %d", reservation.getId());
        String body = String.format("Valor a devolver: %s", formatCurrency(reservation.getRefund()));
        Message message = new Message(
            messageRepo.getNextId(),
            MessageChannel.EMAIL,
            "financeiro@pousadamarealta.com.br",
            subject,
            body
        );
        messageRepo.save(message);
    }

    private String buildGuestConfirmationBody(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(buildComprovante(reservation, room));
        sb.append("\n\nA entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }

    private String buildComprovante(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName())
            .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber())
            .append(" (").append(room.type().displayName).append(")\n");
        sb.append("Entrada: ").append(reservation.getCheckIn().format(DATE_FORMATTER))
            .append(" | Saída: ").append(reservation.getCheckOut().format(DATE_FORMATTER))
            .append(" | ").append(reservation.getNights()).append(" diária");
        if (reservation.getNights() > 1) sb.append("s");
        sb.append("\n");
        sb.append("Diárias: ").append(formatCurrency(reservation.getSubtotal())).append("\n");
        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatCurrency(reservation.getDiscount())).append("\n");
        }
        sb.append("Total: ").append(formatCurrency(reservation.getTotal()));
        return sb.toString();
    }

    private String formatCurrency(BigDecimal value) {
        return String.format("R$ %.2f", value);
    }

    private String formatCpf(String cpf) {
        if (cpf.length() != 11) return cpf;
        return String.format("%s.%s.%s-%s",
            cpf.substring(0, 3),
            cpf.substring(3, 6),
            cpf.substring(6, 9),
            cpf.substring(9, 11)
        );
    }

    private String formatDateRange(LocalDate checkIn, LocalDate checkOut) {
        return String.format("%s a %s",
            checkIn.format(DATE_FORMATTER),
            checkOut.format(DATE_FORMATTER)
        );
    }
}

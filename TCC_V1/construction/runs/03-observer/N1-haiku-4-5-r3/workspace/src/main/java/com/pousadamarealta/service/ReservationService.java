package com.pousadamarealta.service;

import com.pousadamarealta.dto.CreateReservationRequest;
import com.pousadamarealta.model.*;
import com.pousadamarealta.notification.NotificationService;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.util.DateUtils;
import com.pousadamarealta.util.MoneyUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final NotificationService notificationService;

    public ReservationService(ReservationRepository reservationRepository, RoomRepository roomRepository,
                           MessageRepository messageRepository, NotificationService notificationService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.notificationService = notificationService;
    }

    public Reservation createReservation(CreateReservationRequest request) throws Exception {
        validateCreateRequest(request);

        LocalDate checkIn = LocalDate.parse(request.checkIn);
        LocalDate checkOut = LocalDate.parse(request.checkOut);
        Room room = roomRepository.findByNumber(request.roomNumber)
                .orElseThrow(() -> new IllegalArgumentException("ROOM_NOT_FOUND"));

        if (request.guests < 1 || request.guests > room.getCapacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        List<Reservation> conflicting = reservationRepository.findByRoomAndDateRange(
                request.roomNumber, checkIn, checkOut);
        if (!conflicting.isEmpty()) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        int nights = DateUtils.countNights(checkIn, checkOut);
        BigDecimal subtotal = calculateNightsCost(room.getNightlyRate(), checkIn, checkOut);
        BigDecimal discount = calculateDiscount(subtotal, RatePlan.valueOf(request.ratePlan));
        BigDecimal total = subtotal.subtract(discount);

        Reservation reservation = new Reservation(
                0,
                request.roomNumber,
                request.guestName,
                request.guestEmail,
                request.guestCpf,
                checkIn,
                checkOut,
                request.guests,
                RatePlan.valueOf(request.ratePlan),
                nights,
                subtotal,
                discount,
                total,
                ReservationStatus.PENDING,
                BigDecimal.ZERO,
                null
        );

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(long id) throws SQLException {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));
    }

    public Reservation confirmReservation(long id) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setAmountPaid(reservation.getTotal());
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.update(reservation);

        sendConfirmationEmail(reservation);
        notificationService.notifyConfirmed(reservation);

        return reservation;
    }

    public Reservation checkIn(long id) throws SQLException {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation checkOut(long id) throws SQLException {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.update(reservation);

        return reservation;
    }

    public Reservation cancelReservation(long id) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        boolean wasConfirmed = reservation.getStatus() == ReservationStatus.CONFIRMED;

        if (wasConfirmed) {
            BigDecimal refund = calculateRefund(reservation);
            reservation.setRefund(refund);
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.update(reservation);

        if (wasConfirmed) {
            notificationService.notifyCancelled(reservation);
        }

        return reservation;
    }

    private BigDecimal calculateRefund(Reservation reservation) {
        if (reservation.getRatePlan() == RatePlan.NON_REFUNDABLE) {
            return BigDecimal.ZERO;
        }
        return reservation.getAmountPaid();
    }

    private void sendConfirmationEmail(Reservation reservation) throws SQLException {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String document = generateDocument(reservation);
        String body = String.format("""
                Olá, %s!

                Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                %s

                A entrada é a partir das 14h, e a saída, até as 12h.
                Até breve!""",
                reservation.getGuestName(),
                document);

        Message message = new Message(0, "EMAIL", reservation.getGuestEmail(), subject, body);
        messageRepository.save(message);
    }

    public String generateDocument(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        sb.append(String.format("Hóspede: %s (CPF %s)\n", reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));
        sb.append(String.format("Quarto: %d (%s)\n", reservation.getRoomNumber(), getRoomType(reservation.getRoomNumber())));

        String nightsLabel = reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias";
        sb.append(String.format("Entrada: %s | Saída: %s | %s\n",
                DateUtils.formatDate(reservation.getCheckIn()),
                DateUtils.formatDate(reservation.getCheckOut()),
                nightsLabel));

        sb.append(String.format("Diárias: %s\n", MoneyUtils.formatMoneyPrecise(reservation.getSubtotal())));

        if (reservation.getDiscount().signum() > 0) {
            sb.append(String.format("Desconto: %s\n", MoneyUtils.formatMoneyPrecise(reservation.getDiscount())));
        }

        sb.append(String.format("Total: %s", MoneyUtils.formatMoneyPrecise(reservation.getTotal())));

        return sb.toString();
    }

    private String getRoomType(int roomNumber) {
        return switch (roomNumber) {
            case 101, 102 -> "STANDARD";
            case 201, 202 -> "DELUXE";
            case 301 -> "SUITE";
            default -> "UNKNOWN";
        };
    }

    private String formatCpf(String cpf) {
        if (cpf.length() != 11) return cpf;
        return String.format("%s.%s.%s-%s",
                cpf.substring(0, 3),
                cpf.substring(3, 6),
                cpf.substring(6, 9),
                cpf.substring(9));
    }

    private BigDecimal calculateNightsCost(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal totalCost = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightRate = baseRate;
            if (DateUtils.isHighSeason(current)) {
                nightRate = MoneyUtils.applyHighSeasonRate(baseRate);
            }
            totalCost = totalCost.add(nightRate);
            current = current.plusDays(1);
        }

        return MoneyUtils.round(totalCost);
    }

    private BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.STANDARD) {
            return BigDecimal.ZERO;
        }
        return MoneyUtils.calculateDiscount(subtotal, 15);
    }

    public void validateCreateRequest(CreateReservationRequest request) {
        if (request.roomNumber == 0 || request.guestName == null || request.guestEmail == null ||
            request.guestCpf == null || request.checkIn == null || request.checkOut == null ||
            request.ratePlan == null) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        try {
            LocalDate checkIn = LocalDate.parse(request.checkIn);
            LocalDate checkOut = LocalDate.parse(request.checkOut);

            if (!checkOut.isAfter(checkIn)) {
                throw new IllegalArgumentException("INVALID_REQUEST");
            }

            if (request.guests < 1) {
                throw new IllegalArgumentException("INVALID_REQUEST");
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("INVALID_REQUEST")) {
                throw e;
            }
            throw new IllegalArgumentException("INVALID_REQUEST");
        }
    }
}

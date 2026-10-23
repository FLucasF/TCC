package com.pousada.marealta.service;

import com.pousada.marealta.domain.*;
import com.pousada.marealta.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private final PricingService pricingService;
    private final MessageService messageService;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomService roomService,
                            PricingService pricingService,
                            MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
        this.pricingService = pricingService;
        this.messageService = messageService;
    }

    public Reservation createReservation(Reservation reservation) {
        // Valida que o quarto existe
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        if (room == null) {
            throw new RoomNotFoundException();
        }

        // Valida capacidade
        if (reservation.getGuests() > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        // Valida disponibilidade
        List<Reservation> conflicts = reservationRepository.findConflictingReservations(
            reservation.getRoomNumber(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            null
        );
        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException();
        }

        // Calcula preços
        calculatePricing(reservation, room);

        // Salva
        return reservationRepository.save(reservation);
    }

    private void calculatePricing(Reservation reservation, Room room) {
        int nights = (int) ChronoUnit.DAYS.between(reservation.getCheckIn(), reservation.getCheckOut());
        reservation.setNights(nights);

        // Calcula o preço de cada noite e soma
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentNight = reservation.getCheckIn();
        for (int i = 0; i < nights; i++) {
            BigDecimal nightPrice = pricingService.calculateNightPrice(room.getNightlyRate(), currentNight);
            subtotal = subtotal.add(nightPrice);
            currentNight = currentNight.plusDays(1);
        }
        subtotal = pricingService.round(subtotal);
        reservation.setSubtotal(subtotal);

        // Calcula desconto se aplicável
        BigDecimal discount = BigDecimal.ZERO;
        if (reservation.getRatePlan() == RatePlan.NON_REFUNDABLE) {
            discount = subtotal.multiply(new BigDecimal("0.15"));
            discount = pricingService.round(discount);
        }
        reservation.setDiscount(discount);

        // Calcula total
        BigDecimal total = subtotal.subtract(discount);
        total = pricingService.round(total);
        reservation.setTotal(total);
    }

    public Reservation getReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id);
        if (reservation == null) {
            throw new ReservationNotFoundException();
        }
        return reservation;
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canBeConfirmed()) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());
        reservationRepository.save(reservation);

        // Envia e-mail de confirmação
        sendConfirmationEmail(reservation);

        return reservation;
    }

    private void sendConfirmationEmail(Reservation reservation) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationEmailBody(reservation);
        messageService.sendMessage(reservation.getGuestEmail(), subject, body);
    }

    private String buildConfirmationEmailBody(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(buildDocument(reservation));
        sb.append("\n\nA entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canBeCheckedIn()) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservationRepository.save(reservation);
    }

    public String checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canBeCheckedOut()) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.save(reservation);

        return buildDocument(reservation);
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.canBeCancelled()) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        // Calcula reembolso
        BigDecimal refund = BigDecimal.ZERO;
        if (reservation.getRatePlan() == RatePlan.STANDARD) {
            refund = reservation.getAmountPaid();
        }
        reservation.setRefund(refund);

        return reservationRepository.save(reservation);
    }

    public String getDocument(Long id) {
        Reservation reservation = getReservation(id);

        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException();
        }

        return buildDocument(reservation);
    }

    private String buildDocument(Reservation reservation) {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());

        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName());
        sb.append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber());
        sb.append(" (").append(room.getType()).append(")\n");
        sb.append("Entrada: ").append(formatDate(reservation.getCheckIn()));
        sb.append(" | Saída: ").append(formatDate(reservation.getCheckOut()));
        sb.append(" | ").append(reservation.getNights());
        sb.append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(reservation.getTotal()));

        return sb.toString();
    }

    private String formatCpf(String cpf) {
        // Remove tudo que não é dígito
        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() == 11) {
            return digits.substring(0, 3) + "." +
                   digits.substring(3, 6) + "." +
                   digits.substring(6, 9) + "-" +
                   digits.substring(9);
        }
        return cpf;
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d",
            date.getDayOfMonth(),
            date.getMonthValue(),
            date.getYear());
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }

    // Exceções
    public static class RoomNotFoundException extends RuntimeException {}
    public static class ReservationNotFoundException extends RuntimeException {}
    public static class RoomUnavailableException extends RuntimeException {}
    public static class CapacityExceededException extends RuntimeException {}
    public static class OperationNotAllowedException extends RuntimeException {}
}

package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReservationService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final PricingService pricingService;
    private final MessageService messageService;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomRepository roomRepository,
                            PricingService pricingService,
                            MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.pricingService = pricingService;
        this.messageService = messageService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException());

        if (guests < 1) {
            throw new InvalidRequestException("Número de hóspedes deve ser no mínimo 1");
        }

        if (guests > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidRequestException("Data de saída deve ser após a data de entrada");
        }

        LocalDate night = checkIn;
        while (night.isBefore(checkOut)) {
            List<Reservation> existing = reservationRepository.findByRoomAndNight(roomNumber, night);
            if (!existing.isEmpty()) {
                throw new RoomUnavailableException();
            }
            night = night.plusDays(1);
        }

        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = pricingService.calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = pricingService.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(
                reservationRepository.nextId(),
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

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = findById(id);

        if (!reservation.canConfirm()) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());

        sendConfirmationEmail(reservation);

        return reservationRepository.save(reservation);
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = findById(id);

        if (!reservation.canCancel()) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        BigDecimal refundAmount = BigDecimal.ZERO;
        if (reservation.getRatePlan() == RatePlan.STANDARD) {
            refundAmount = reservation.getAmountPaid();
        }

        reservation.setRefund(refundAmount);

        return reservationRepository.save(reservation);
    }

    public Reservation checkIn(long id) {
        Reservation reservation = findById(id);

        if (!reservation.canCheckIn()) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);

        return reservationRepository.save(reservation);
    }

    public Map<String, Object> checkOut(long id) {
        Reservation reservation = findById(id);

        if (!reservation.canCheckOut()) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.save(reservation);

        return generateDocument(reservation);
    }

    public Map<String, Object> getDocument(long id) {
        Reservation reservation = findById(id);

        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException();
        }

        return generateDocument(reservation);
    }

    public Reservation findById(long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException());
    }

    private void sendConfirmationEmail(Reservation reservation) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String document = formatDocumentText(reservation);

        String body = String.format(
                "Olá, %s!\n\n" +
                "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                "%s\n\n" +
                "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                "Até breve!",
                reservation.getGuestName(),
                document
        );

        messageService.send(MessageChannel.EMAIL, reservation.getGuestEmail(), subject, body);
    }

    private Map<String, Object> generateDocument(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
                .orElseThrow(() -> new RoomNotFoundException());

        Map<String, Object> document = new HashMap<>();
        document.put("title", "Comprovante de reserva");
        document.put("reservationId", reservation.getId());
        document.put("guestName", reservation.getGuestName());
        document.put("guestCpf", reservation.getGuestCpf());
        document.put("roomNumber", reservation.getRoomNumber());
        document.put("roomType", room.getType().name());
        document.put("checkIn", reservation.getCheckIn());
        document.put("checkOut", reservation.getCheckOut());
        document.put("nights", reservation.getNights());
        document.put("subtotal", reservation.getSubtotal());
        document.put("discount", reservation.getDiscount());
        document.put("total", reservation.getTotal());

        return document;
    }

    private String formatDocumentText(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
                .orElseThrow(() -> new RoomNotFoundException());

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        sb.append(String.format("Hóspede: %s (CPF %s)\n", reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));
        sb.append(String.format("Quarto: %s (%s)\n", reservation.getRoomNumber(), room.getType().name()));
        sb.append(String.format("Entrada: %s | Saída: %s | %d %s\n",
                reservation.getCheckIn().format(DATE_FORMATTER),
                reservation.getCheckOut().format(DATE_FORMATTER),
                reservation.getNights(),
                reservation.getNights() == 1 ? "diária" : "diárias"));
        sb.append(String.format("Diárias: %s\n", formatMoney(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s\n", formatMoney(reservation.getDiscount())));
        }

        sb.append(String.format("Total: %s", formatMoney(reservation.getTotal())));

        return sb.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }

    public static class RoomNotFoundException extends RuntimeException {}
    public static class ReservationNotFoundException extends RuntimeException {}
    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }
    public static class CapacityExceededException extends RuntimeException {}
    public static class RoomUnavailableException extends RuntimeException {}
    public static class OperationNotAllowedException extends RuntimeException {}
}

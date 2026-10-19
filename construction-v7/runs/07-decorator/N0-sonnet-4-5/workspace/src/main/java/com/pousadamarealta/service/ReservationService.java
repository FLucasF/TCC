package com.pousadamarealta.service;

import com.pousadamarealta.model.*;
import com.pousadamarealta.util.FormatUtil;
import com.pousadamarealta.util.PricingUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ReservationService {
    private final Map<Long, Reservation> reservations = new LinkedHashMap<>();
    private final AtomicLong reservationIdCounter = new AtomicLong(1);
    private final RoomService roomService;
    private final MessageService messageService;

    public ReservationService(RoomService roomService, MessageService messageService) {
        this.roomService = roomService;
        this.messageService = messageService;
    }

    public Reservation createReservation(String roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Room room = roomService.getRoom(roomNumber);

        if (guests < 1) {
            throw ApiException.invalidRequest("Número de hóspedes deve ser pelo menos 1");
        }

        if (guests > room.getCapacity()) {
            throw ApiException.capacityExceeded();
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw ApiException.invalidRequest("Data de saída deve ser após a data de entrada");
        }

        if (!isRoomAvailable(roomNumber, checkIn, checkOut, null)) {
            throw ApiException.roomUnavailable();
        }

        int nights = (int) checkIn.until(checkOut).getDays();

        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentNight = checkIn;
        while (currentNight.isBefore(checkOut)) {
            BigDecimal nightPrice = PricingUtil.calculateNightlyPrice(room.getNightlyRate(), currentNight);
            subtotal = subtotal.add(nightPrice);
            currentNight = currentNight.plusDays(1);
        }
        subtotal = PricingUtil.round(subtotal);

        BigDecimal discount = PricingUtil.calculateDiscount(subtotal, ratePlan == RatePlan.NON_REFUNDABLE);
        BigDecimal total = PricingUtil.round(subtotal.subtract(discount));

        Reservation reservation = new Reservation(
            reservationIdCounter.getAndIncrement(),
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

        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public Reservation getReservation(Long id) {
        Reservation reservation = reservations.get(id);
        if (reservation == null) {
            throw ApiException.reservationNotFound();
        }
        return reservation;
    }

    public Reservation confirmReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw ApiException.operationNotAllowed();
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(reservation.getTotal());

        sendConfirmationEmail(reservation);

        return reservation;
    }

    public Reservation cancelReservation(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING
            && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw ApiException.operationNotAllowed();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        BigDecimal refundAmount = BigDecimal.ZERO;
        if (reservation.getRatePlan() == RatePlan.STANDARD) {
            refundAmount = reservation.getAmountPaid();
        }
        reservation.setRefund(refundAmount);

        return reservation;
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw ApiException.operationNotAllowed();
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservation;
    }

    public Document checkOut(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw ApiException.operationNotAllowed();
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return generateDocument(reservation);
    }

    public Document getDocument(Long id) {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.PENDING
            || reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw ApiException.operationNotAllowed();
        }

        return generateDocument(reservation);
    }

    private Document generateDocument(Reservation reservation) {
        Room room = roomService.getRoom(reservation.getRoomNumber());

        return new Document(
            "Comprovante de reserva",
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
    }

    private void sendConfirmationEmail(Reservation reservation) {
        Document document = generateDocument(reservation);
        String receipt = formatReceipt(document);

        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""",
            reservation.getGuestName(),
            receipt);

        messageService.sendEmail(
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );
    }

    private String formatReceipt(Document document) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("Comprovante de reserva nº ").append(document.getReservationId()).append("\n");
        receipt.append("Hóspede: ").append(document.getGuestName())
               .append(" (CPF ").append(FormatUtil.formatCpf(document.getGuestCpf())).append(")\n");
        receipt.append("Quarto: ").append(document.getRoomNumber())
               .append(" (").append(document.getRoomType()).append(")\n");
        receipt.append("Entrada: ").append(FormatUtil.formatDate(document.getCheckIn()))
               .append(" | Saída: ").append(FormatUtil.formatDate(document.getCheckOut()))
               .append(" | ").append(document.getNights())
               .append(document.getNights() == 1 ? " diária" : " diárias").append("\n");
        receipt.append("Diárias: ").append(FormatUtil.formatMoney(document.getSubtotal())).append("\n");

        if (document.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append("Desconto: ").append(FormatUtil.formatMoney(document.getDiscount())).append("\n");
        }

        receipt.append("Total: ").append(FormatUtil.formatMoney(document.getTotal()));

        return receipt.toString();
    }

    private boolean isRoomAvailable(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        return reservations.values().stream()
            .filter(r -> !r.getId().equals(excludeReservationId))
            .filter(r -> r.getRoomNumber().equals(roomNumber))
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
            .noneMatch(r -> datesOverlap(checkIn, checkOut, r.getCheckIn(), r.getCheckOut()));
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    public List<Reservation> getAllReservations() {
        return new ArrayList<>(reservations.values());
    }

    public List<Reservation> getReservationsForNight(LocalDate night) {
        return reservations.values().stream()
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED
                      || r.getStatus() == ReservationStatus.CHECKED_IN
                      || r.getStatus() == ReservationStatus.CHECKED_OUT)
            .filter(r -> !r.getCheckIn().isAfter(night) && r.getCheckOut().isAfter(night))
            .sorted(Comparator.comparing(Reservation::getRoomNumber))
            .collect(Collectors.toList());
    }

    public List<Reservation> getReservationsForMonth(int year, int month) {
        return reservations.values().stream()
            .filter(r -> r.getCheckIn().getYear() == year && r.getCheckIn().getMonthValue() == month)
            .filter(r -> r.getAmountPaid().compareTo(BigDecimal.ZERO) > 0
                      || (r.getRefund() != null && r.getRefund().compareTo(BigDecimal.ZERO) > 0))
            .sorted(Comparator.comparing(Reservation::getCheckIn)
                             .thenComparing(Reservation::getId))
            .collect(Collectors.toList());
    }
}

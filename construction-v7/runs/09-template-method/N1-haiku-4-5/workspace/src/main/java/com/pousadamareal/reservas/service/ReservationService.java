package com.pousadamareal.reservas.service;

import com.pousadamareal.reservas.domain.*;
import com.pousadamareal.reservas.dto.*;
import com.pousadamareal.reservas.util.PricingCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ReservationService {
    private static final List<Room> ROOMS = List.of(
        new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90")),
        new Room(102, RoomType.STANDARD, 2, new BigDecimal("249.90")),
        new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90")),
        new Room(202, RoomType.DELUXE, 3, new BigDecimal("389.90")),
        new Room(301, RoomType.SUITE, 4, new BigDecimal("579.90"))
    );

    private final Map<Long, Reservation> reservations = new HashMap<>();
    private final List<Message> messages = new ArrayList<>();
    private long reservationIdCounter = 1;
    private long messageIdCounter = 1;

    public List<RoomResponse> getAllRooms() {
        return ROOMS.stream()
            .map(room -> {
                RoomResponse response = new RoomResponse();
                response.number = room.getNumber();
                response.type = room.getType().getValue();
                response.capacity = room.getCapacity();
                response.nightlyRate = room.getNightlyRate();
                return response;
            })
            .collect(Collectors.toList());
    }

    public Room getRoomByNumber(int roomNumber) {
        return ROOMS.stream()
            .filter(room -> room.getNumber() == roomNumber)
            .findFirst()
            .orElse(null);
    }

    public ReservationResponse createReservation(CreateReservationRequest request) {
        Room room = getRoomByNumber(request.roomNumber);
        if (room == null) {
            throw new ApiException(404, "ROOM_NOT_FOUND", "Room not found");
        }

        if (request.checkIn.isAfter(request.checkOut) || request.checkIn.equals(request.checkOut)) {
            throw new ApiException(400, "INVALID_REQUEST", "Check-out must be after check-in");
        }

        if (request.guests < 1) {
            throw new ApiException(400, "INVALID_REQUEST", "At least one guest is required");
        }

        if (request.guests > room.getCapacity()) {
            throw new ApiException(422, "CAPACITY_EXCEEDED", "Too many guests for this room");
        }

        if (isRoomUnavailable(request.roomNumber, request.checkIn, request.checkOut)) {
            throw new ApiException(409, "ROOM_UNAVAILABLE", "Room is not available for these dates");
        }

        RatePlan ratePlan;
        try {
            ratePlan = RatePlan.fromString(request.ratePlan);
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid rate plan");
        }

        long nights = PricingCalculator.calculateNights(request.checkIn, request.checkOut);
        BigDecimal subtotal = PricingCalculator.calculateSubtotal(room.getNightlyRate(), request.checkIn, request.checkOut);
        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, ratePlan == RatePlan.NON_REFUNDABLE);
        BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);

        long id = reservationIdCounter++;
        Reservation reservation = new Reservation(
            id, request.roomNumber, request.guestName, request.guestEmail, request.guestCpf,
            request.checkIn, request.checkOut, request.guests, ratePlan, nights, subtotal, discount, total
        );

        reservations.put(id, reservation);
        return mapReservationToResponse(reservation);
    }

    public ReservationResponse getReservation(long id) {
        Reservation reservation = reservations.get(id);
        if (reservation == null) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
        }
        return mapReservationToResponse(reservation);
    }

    public ReservationResponse confirmReservation(long id, BigDecimal amountPaid) {
        Reservation reservation = reservations.get(id);
        if (reservation == null) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Can only confirm pending reservations");
        }

        reservation.confirm(amountPaid);

        String comprovante = generateComprovante(reservation);
        sendEmail(reservation.getGuestEmail(),
                 "Reserva confirmada - Pousada Maré Alta",
                 generateConfirmationEmailBody(reservation, comprovante));

        return mapReservationToResponse(reservation);
    }

    public ReservationResponse checkIn(long id) {
        Reservation reservation = reservations.get(id);
        if (reservation == null) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Can only check in confirmed reservations");
        }

        reservation.checkIn();
        return mapReservationToResponse(reservation);
    }

    public DocumentResponse checkOut(long id) {
        Reservation reservation = reservations.get(id);
        if (reservation == null) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Can only check out reservations that are checked in");
        }

        reservation.checkOut();
        return mapReservationToDocument(reservation);
    }

    public ReservationResponse cancelReservation(long id) {
        Reservation reservation = reservations.get(id);
        if (reservation == null) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
        }

        ReservationStatus status = reservation.getStatus();
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Can only cancel pending or confirmed reservations");
        }

        BigDecimal refund = BigDecimal.ZERO;
        if (status == ReservationStatus.CONFIRMED) {
            if (reservation.getRatePlan() == RatePlan.STANDARD) {
                refund = reservation.getAmountPaid();
            }
        }

        reservation.cancel(refund);
        return mapReservationToResponse(reservation);
    }

    public DocumentResponse getDocument(long id) {
        Reservation reservation = reservations.get(id);
        if (reservation == null) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
        }

        if (reservation.getStatus() == ReservationStatus.PENDING) {
            throw new ApiException(409, "OPERATION_NOT_ALLOWED", "Cannot get document for pending reservations");
        }

        return mapReservationToDocument(reservation);
    }

    public List<MessageResponse> getMessages() {
        return messages.stream()
            .map(msg -> {
                MessageResponse response = new MessageResponse();
                response.id = msg.getId();
                response.channel = msg.getChannel();
                response.to = msg.getTo();
                response.subject = msg.getSubject();
                response.body = msg.getBody();
                return response;
            })
            .collect(Collectors.toList());
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return reservations.values().stream()
            .filter(r -> r.getRoomNumber() == roomNumber)
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
            .anyMatch(r -> datesOverlap(r.getCheckIn(), r.getCheckOut(), checkIn, checkOut));
    }

    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    private ReservationResponse mapReservationToResponse(Reservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.id = reservation.getId();
        response.roomNumber = reservation.getRoomNumber();
        response.guestName = reservation.getGuestName();
        response.guestEmail = reservation.getGuestEmail();
        response.guestCpf = reservation.getGuestCpf();
        response.checkIn = reservation.getCheckIn();
        response.checkOut = reservation.getCheckOut();
        response.guests = reservation.getGuests();
        response.ratePlan = reservation.getRatePlan().getValue();
        response.nights = reservation.getNights();
        response.subtotal = reservation.getSubtotal();
        response.discount = reservation.getDiscount();
        response.total = reservation.getTotal();
        response.status = reservation.getStatus().getValue();
        response.amountPaid = reservation.getAmountPaid();
        response.refund = reservation.getRefund();
        return response;
    }

    private DocumentResponse mapReservationToDocument(Reservation reservation) {
        Room room = getRoomByNumber(reservation.getRoomNumber());
        DocumentResponse response = new DocumentResponse();
        response.title = "Comprovante de reserva";
        response.reservationId = reservation.getId();
        response.guestName = reservation.getGuestName();
        response.guestCpf = reservation.getGuestCpf();
        response.roomNumber = reservation.getRoomNumber();
        response.roomType = room.getType().getValue();
        response.checkIn = reservation.getCheckIn();
        response.checkOut = reservation.getCheckOut();
        response.nights = reservation.getNights();
        response.subtotal = reservation.getSubtotal();
        response.discount = reservation.getDiscount();
        response.total = reservation.getTotal();
        return response;
    }

    private String generateComprovante(Reservation reservation) {
        Room room = getRoomByNumber(reservation.getRoomNumber());
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName())
            .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(room.getNumber())
            .append(" (").append(room.getType().getValue()).append(")\n");
        sb.append("Entrada: ").append(formatDate(reservation.getCheckIn()))
            .append(" | Saída: ").append(formatDate(reservation.getCheckOut()))
            .append(" | ").append(reservation.getNights())
            .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatCurrency(reservation.getSubtotal())).append("\n");
        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatCurrency(reservation.getDiscount())).append("\n");
        }
        sb.append("Total: ").append(formatCurrency(reservation.getTotal()));
        return sb.toString();
    }

    private String generateConfirmationEmailBody(Reservation reservation, String comprovante) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(comprovante).append("\n\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }

    private void sendEmail(String to, String subject, String body) {
        long id = messageIdCounter++;
        Message message = new Message(id, "EMAIL", to, subject, body);
        messages.add(message);
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }

    private String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return String.format("%s.%s.%s-%s",
            cpf.substring(0, 3),
            cpf.substring(3, 6),
            cpf.substring(6, 9),
            cpf.substring(9, 11));
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder sb = new StringBuilder();
        sb.append("Pousada Maré Alta,occupancy,").append(formatDateForReport(date)).append("\n");
        sb.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> occupiedReservations = reservations.values().stream()
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                        r.getStatus() == ReservationStatus.CHECKED_IN ||
                        r.getStatus() == ReservationStatus.CHECKED_OUT)
            .filter(r -> !r.getCheckIn().isAfter(date) && r.getCheckOut().isAfter(date))
            .sorted(Comparator.comparingInt(Reservation::getRoomNumber))
            .collect(Collectors.toList());

        for (Reservation reservation : occupiedReservations) {
            Room room = getRoomByNumber(reservation.getRoomNumber());
            sb.append(room.getNumber()).append(",")
              .append(room.getType().getValue()).append(",")
              .append(reservation.getId()).append(",")
              .append(formatDateForReport(reservation.getCheckIn())).append(",")
              .append(formatDateForReport(reservation.getCheckOut())).append("\n");
        }

        sb.append("total,").append(occupiedReservations.size()).append("/").append(ROOMS.size()).append("\n");
        return sb.toString();
    }

    public String generateRevenueReport(String month) {
        StringBuilder sb = new StringBuilder();
        sb.append("Pousada Maré Alta,revenue,").append(month).append("\n");
        sb.append("reservation,room,check_in,check_out,status,amount\n");

        LocalDate monthStart = LocalDate.parse(month + "-01");
        BigDecimal totalAmount = BigDecimal.ZERO;

        List<Reservation> monthReservations = reservations.values().stream()
            .filter(r -> r.getCheckIn().getYear() == monthStart.getYear() &&
                        r.getCheckIn().getMonthValue() == monthStart.getMonthValue())
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                        r.getStatus() == ReservationStatus.CHECKED_IN ||
                        r.getStatus() == ReservationStatus.CHECKED_OUT ||
                        (r.getStatus() == ReservationStatus.CANCELLED && r.getRatePlan() == RatePlan.NON_REFUNDABLE))
            .sorted(Comparator.comparing(Reservation::getCheckIn).thenComparingLong(Reservation::getId))
            .collect(Collectors.toList());

        for (Reservation reservation : monthReservations) {
            BigDecimal amount = reservation.getAmountPaid().subtract(
                reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO
            );
            sb.append(reservation.getId()).append(",")
              .append(reservation.getRoomNumber()).append(",")
              .append(formatDateForReport(reservation.getCheckIn())).append(",")
              .append(formatDateForReport(reservation.getCheckOut())).append(",")
              .append(reservation.getStatus().getValue()).append(",")
              .append(formatAmountForReport(amount)).append("\n");
            totalAmount = totalAmount.add(amount);
        }

        sb.append("total,").append(formatAmountForReport(totalAmount)).append("\n");
        return sb.toString();
    }

    public String generateTourismTaxReport(String month) {
        StringBuilder sb = new StringBuilder();
        sb.append("Pousada Maré Alta,tourism-tax,").append(month).append("\n");
        sb.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        LocalDate monthStart = LocalDate.parse(month + "-01");
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal taxPerGuestPerNight = new BigDecimal("4.50");

        List<Reservation> monthReservations = reservations.values().stream()
            .filter(r -> r.getCheckIn().getYear() == monthStart.getYear() &&
                        r.getCheckIn().getMonthValue() == monthStart.getMonthValue())
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                        r.getStatus() == ReservationStatus.CHECKED_IN ||
                        r.getStatus() == ReservationStatus.CHECKED_OUT)
            .sorted(Comparator.comparing(Reservation::getCheckIn).thenComparingLong(Reservation::getId))
            .collect(Collectors.toList());

        for (Reservation reservation : monthReservations) {
            BigDecimal tax = taxPerGuestPerNight
                .multiply(new BigDecimal(reservation.getGuests()))
                .multiply(new BigDecimal(reservation.getNights()));
            tax = PricingCalculator.roundHalfEven(tax);

            sb.append(reservation.getId()).append(",")
              .append(reservation.getRoomNumber()).append(",")
              .append(formatDateForReport(reservation.getCheckIn())).append(",")
              .append(formatDateForReport(reservation.getCheckOut())).append(",")
              .append(reservation.getGuests()).append(",")
              .append(reservation.getNights()).append(",")
              .append(formatAmountForReport(tax)).append("\n");
            totalTax = totalTax.add(tax);
        }

        sb.append("total,").append(formatAmountForReport(totalTax)).append("\n");
        return sb.toString();
    }

    public String generateRatePlansReport(String month) {
        StringBuilder sb = new StringBuilder();
        sb.append("Pousada Maré Alta,rate-plans,").append(month).append("\n");
        sb.append("rate_plan,reservations,amount\n");

        LocalDate monthStart = LocalDate.parse(month + "-01");

        Map<RatePlan, Long> reservationCounts = new HashMap<>();
        Map<RatePlan, BigDecimal> amounts = new HashMap<>();

        for (RatePlan plan : RatePlan.values()) {
            reservationCounts.put(plan, 0L);
            amounts.put(plan, BigDecimal.ZERO);
        }

        List<Reservation> monthReservations = reservations.values().stream()
            .filter(r -> r.getCheckIn().getYear() == monthStart.getYear() &&
                        r.getCheckIn().getMonthValue() == monthStart.getMonthValue())
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                        r.getStatus() == ReservationStatus.CHECKED_IN ||
                        r.getStatus() == ReservationStatus.CHECKED_OUT ||
                        (r.getStatus() == ReservationStatus.CANCELLED && r.getRatePlan() == RatePlan.NON_REFUNDABLE))
            .collect(Collectors.toList());

        for (Reservation reservation : monthReservations) {
            RatePlan plan = reservation.getRatePlan();
            reservationCounts.put(plan, reservationCounts.get(plan) + 1);

            BigDecimal amount = reservation.getAmountPaid().subtract(
                reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO
            );
            amounts.put(plan, amounts.get(plan).add(amount));
        }

        BigDecimal totalCount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (RatePlan plan : RatePlan.values()) {
            long count = reservationCounts.get(plan);
            BigDecimal amount = amounts.get(plan);
            sb.append(plan.getValue()).append(",")
              .append(count).append(",")
              .append(formatAmountForReport(amount)).append("\n");
            totalCount = totalCount.add(new BigDecimal(count));
            totalAmount = totalAmount.add(amount);
        }

        sb.append("total,").append(totalCount.toPlainString()).append(",")
          .append(formatAmountForReport(totalAmount)).append("\n");
        return sb.toString();
    }

    private String formatDateForReport(LocalDate date) {
        return date.toString();
    }

    private String formatAmountForReport(BigDecimal amount) {
        return amount.toPlainString();
    }
}

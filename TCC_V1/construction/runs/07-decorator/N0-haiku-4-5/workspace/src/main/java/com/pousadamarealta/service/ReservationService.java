package com.pousadamarealta.service;

import com.pousadamarealta.model.*;
import com.pousadamarealta.util.DateUtils;
import com.pousadamarealta.util.Money;

import java.time.LocalDate;
import java.util.*;

public class ReservationService {
    private final List<Reservation> reservations = new ArrayList<>();
    private final Map<Integer, Room> rooms = new HashMap<>();
    private long reservationIdCounter = 1;
    private final MessageService messageService;

    public ReservationService(MessageService messageService) {
        this.messageService = messageService;
        initializeRooms();
    }

    private void initializeRooms() {
        rooms.put(101, new Room(101, RoomType.STANDARD, 2, 249.90));
        rooms.put(102, new Room(102, RoomType.STANDARD, 2, 249.90));
        rooms.put(201, new Room(201, RoomType.DELUXE, 3, 389.90));
        rooms.put(202, new Room(202, RoomType.DELUXE, 3, 389.90));
        rooms.put(301, new Room(301, RoomType.SUITE, 4, 579.90));
    }

    public List<Room> getRooms() {
        return new ArrayList<>(rooms.values());
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                         LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Room room = rooms.get(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        if (guests < 1 || guests > room.getCapacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        int nights = DateUtils.daysInRange(checkIn, checkOut);
        double subtotal = calculateSubtotal(room, checkIn, checkOut);
        double discount = calculateDiscount(subtotal, ratePlan);
        double total = Money.subtract(subtotal, discount);

        Reservation reservation = new Reservation(
                reservationIdCounter++,
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

        reservations.add(reservation);
        return reservation;
    }

    public Reservation getReservation(long id) {
        return reservations.stream()
                .filter(r -> r.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public Reservation confirmReservation(long id, double amountPaid) {
        Reservation reservation = getReservation(id);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(amountPaid);

        String comprovante = generateComprovante(reservation);
        sendConfirmationEmail(reservation, comprovante);

        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservation;
    }

    public String checkOut(long id) {
        Reservation reservation = getReservation(id);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return generateComprovante(reservation);
    }

    public String getDocument(long id) {
        Reservation reservation = getReservation(id);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED &&
            reservation.getStatus() != ReservationStatus.CHECKED_IN &&
            reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        return generateComprovante(reservation);
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }

        ReservationStatus status = reservation.getStatus();
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        double refundAmount = 0;

        if (status == ReservationStatus.CONFIRMED) {
            if (reservation.getRatePlan() == RatePlan.STANDARD) {
                refundAmount = reservation.getAmountPaid();
            }
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        if (refundAmount > 0) {
            reservation.setRefund(refundAmount);
        }

        return reservation;
    }

    private double calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        double total = 0;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            double nightPrice = room.getNightlyRate();
            if (DateUtils.isHighSeason(current)) {
                nightPrice = Money.round(nightPrice * 1.2);
            } else {
                nightPrice = Money.round(nightPrice);
            }
            total = Money.add(total, nightPrice);
            current = current.plusDays(1);
        }

        return Money.round(total);
    }

    private double calculateDiscount(double subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return Money.applyPercentageDiscount(subtotal, 15);
        }
        return 0;
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            LocalDate night = current;
            boolean occupied = reservations.stream()
                    .filter(r -> r.getRoomNumber() == roomNumber)
                    .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                    .anyMatch(r -> !night.isBefore(r.getCheckIn()) && night.isBefore(r.getCheckOut()));

            if (occupied) {
                return true;
            }

            current = current.plusDays(1);
        }
        return false;
    }

    private String generateComprovante(Reservation reservation) {
        Room room = rooms.get(reservation.getRoomNumber());
        StringBuilder sb = new StringBuilder();

        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(room.getType()).append(")\n");
        sb.append("Entrada: ").append(DateUtils.formatForDisplay(reservation.getCheckIn())).append(" | ");
        sb.append("Saída: ").append(DateUtils.formatForDisplay(reservation.getCheckOut())).append(" | ");
        sb.append(reservation.getNights()).append(" ").append(reservation.getNights() == 1 ? "diária" : "diárias").append("\n");
        sb.append("Diárias: ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount() > 0) {
            sb.append("Desconto: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(reservation.getTotal()));

        return sb.toString();
    }

    private void sendConfirmationEmail(Reservation reservation, String comprovante) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = "Olá, " + reservation.getGuestName() + "!\n\n" +
                "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                comprovante + "\n\n" +
                "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                "Até breve!";

        messageService.sendEmail(reservation.getGuestEmail(), subject, body);
    }

    private String formatMoney(double value) {
        return String.format("R$ %.2f", value).replace(",", "X").replace(".", ",").replace("X", ".");
    }

    private String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    public List<Reservation> getReservations() {
        return new ArrayList<>(reservations);
    }

    public void clear() {
        reservations.clear();
        reservationIdCounter = 1;
    }
}

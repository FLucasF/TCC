package com.pousadamareaalta.service;

import com.pousadamareaalta.model.Message;
import com.pousadamareaalta.model.Reservation;
import com.pousadamareaalta.model.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class ReservationService {
    private final Map<Integer, Room> rooms = new HashMap<>();
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private final List<Message> messages = new ArrayList<>();
    private long nextReservationId = 1;
    private long nextMessageId = 1;
    private final PricingService pricingService;

    public ReservationService() {
        this.pricingService = new PricingService();
        initializeRooms();
    }

    private void initializeRooms() {
        rooms.put(101, new Room(101, "STANDARD", 2, new BigDecimal("249.90")));
        rooms.put(102, new Room(102, "STANDARD", 2, new BigDecimal("249.90")));
        rooms.put(201, new Room(201, "DELUXE", 3, new BigDecimal("389.90")));
        rooms.put(202, new Room(202, "DELUXE", 3, new BigDecimal("389.90")));
        rooms.put(301, new Room(301, "SUITE", 4, new BigDecimal("579.90")));
    }

    public Collection<Room> getRooms() {
        return rooms.values();
    }

    public Room getRoom(int number) {
        return rooms.get(number);
    }

    public long calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, Reservation.RatePlan ratePlan) {
        Room room = rooms.get(roomNumber);

        if (room == null) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        if (guests < 1 || guests > room.getCapacity()) {
            throw new IllegalArgumentException(guests > room.getCapacity() ? "CAPACITY_EXCEEDED" : "INVALID_REQUEST");
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        long nights = calculateNights(checkIn, checkOut);
        BigDecimal nightlyPrice = pricingService.calculateNightlyPrice(room.getNightlyRate(), checkIn);
        BigDecimal subtotal = pricingService.calculateSubtotal(nightlyPrice, nights);
        boolean isNonRefundable = ratePlan == Reservation.RatePlan.NON_REFUNDABLE;
        BigDecimal discount = pricingService.calculateDiscount(subtotal, isNonRefundable);
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation(nextReservationId++, roomNumber, guestName,
                guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total);

        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        for (Reservation res : reservations.values()) {
            if (res.getRoomNumber() == roomNumber && res.getStatus() != Reservation.Status.CANCELLED) {
                if (overlapsWithReservation(checkIn, checkOut, res.getCheckIn(), res.getCheckOut())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean overlapsWithReservation(LocalDate checkIn, LocalDate checkOut,
                                           LocalDate resCheckIn, LocalDate resCheckOut) {
        return !checkIn.isAfter(resCheckOut.minusDays(1)) && !checkOut.isBefore(resCheckIn.plusDays(1));
    }

    public Reservation getReservation(long id) {
        return reservations.get(id);
    }

    public Map<Long, Reservation> getReservations() {
        return reservations;
    }

    public void confirmReservation(long id, BigDecimal amountPaid) {
        Reservation res = reservations.get(id);
        if (res == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }
        if (res.getStatus() != Reservation.Status.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }
        res.setStatus(Reservation.Status.CONFIRMED);
        res.setAmountPaid(amountPaid);

        String receipt = generateReceipt(res);
        String emailBody = generateConfirmationEmail(res, receipt);
        sendMessage(Message.Channel.EMAIL, res.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta", emailBody);
    }

    public void checkIn(long id) {
        Reservation res = reservations.get(id);
        if (res == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }
        if (res.getStatus() != Reservation.Status.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }
        res.setStatus(Reservation.Status.CHECKED_IN);
    }

    public void checkOut(long id) {
        Reservation res = reservations.get(id);
        if (res == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }
        if (res.getStatus() != Reservation.Status.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }
        res.setStatus(Reservation.Status.CHECKED_OUT);
    }

    public void cancelReservation(long id) {
        Reservation res = reservations.get(id);
        if (res == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }
        if (res.getStatus() != Reservation.Status.PENDING && res.getStatus() != Reservation.Status.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        BigDecimal refund = BigDecimal.ZERO;
        if (res.getStatus() == Reservation.Status.CONFIRMED) {
            if (res.getRatePlan() == Reservation.RatePlan.STANDARD) {
                refund = res.getAmountPaid();
            }
        }

        res.setStatus(Reservation.Status.CANCELLED);
        if (refund.compareTo(BigDecimal.ZERO) > 0) {
            res.setRefund(refund);
        }
    }

    public String generateReceipt(Reservation res) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(res.getId()).append("\n");
        sb.append("Hóspede: ").append(res.getGuestName()).append(" (CPF ").append(res.getGuestCpf()).append(")\n");
        sb.append("Quarto: ").append(res.getRoomNumber()).append(" (").append(getRoom(res.getRoomNumber()).getType()).append(")\n");
        sb.append("Entrada: ").append(formatDate(res.getCheckIn()));
        sb.append(" | Saída: ").append(formatDate(res.getCheckOut()));
        sb.append(" | ").append(res.getNights()).append(res.getNights() == 1 ? " diária\n" : " diárias\n");
        sb.append("Diárias: ").append(formatMoney(res.getSubtotal())).append("\n");
        if (res.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(res.getDiscount())).append("\n");
        }
        sb.append("Total: ").append(formatMoney(res.getTotal()));
        return sb.toString();
    }

    private String generateConfirmationEmail(Reservation res, String receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(res.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(receipt).append("\n\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }

    public void sendMessage(Message.Channel channel, String to, String subject, String body) {
        Message msg = new Message(nextMessageId++, channel, to, subject, body);
        messages.add(msg);
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    public String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    public String formatMoney(BigDecimal value) {
        return String.format("R$ %,.2f", value).replace(",", ".");
    }

    public String formatMoneyForCsv(BigDecimal value) {
        return String.format("%.2f", value);
    }
}

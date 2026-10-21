package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Receipt;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.util.DateUtil;
import com.pousadamarealta.util.MoneyUtil;
import com.pousadamarealta.util.PriceCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationService {
    private final List<Reservation> reservations;
    private final AtomicLong reservationIdCounter;
    private final RoomService roomService;
    private final MessageService messageService;

    public ReservationService(RoomService roomService, MessageService messageService) {
        this.reservations = new ArrayList<>();
        this.reservationIdCounter = new AtomicLong(0);
        this.roomService = roomService;
        this.messageService = messageService;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Optional<Room> room = roomService.getRoomByNumber(roomNumber);
        if (room.isEmpty()) {
            throw new IllegalArgumentException("Room not found");
        }

        Room r = room.get();

        if (guests < 1 || guests > r.getCapacity()) {
            throw new IllegalArgumentException("Invalid guest count");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }

        if (isRoomUnavailable(roomNumber, checkIn, checkOut)) {
            throw new IllegalArgumentException("Room unavailable for those dates");
        }

        PriceCalculator.PriceBreakdown prices = PriceCalculator.calculate(r.getNightlyRate(), checkIn, checkOut, ratePlan);

        long resId = reservationIdCounter.incrementAndGet();
        Reservation reservation = new Reservation(
                resId, roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan,
                prices.nights, prices.subtotal, prices.discount, prices.total
        );

        reservations.add(reservation);
        return reservation;
    }

    public Optional<Reservation> getReservation(long id) {
        return reservations.stream()
                .filter(r -> r.getId() == id)
                .findFirst();
    }

    public Reservation confirmReservation(long id, BigDecimal amountPaid) {
        Optional<Reservation> opt = getReservation(id);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Reservation not found");
        }

        Reservation reservation = opt.get();

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("Cannot confirm a non-pending reservation");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(amountPaid);

        Optional<Room> room = roomService.getRoomByNumber(reservation.getRoomNumber());
        if (room.isPresent()) {
            Receipt receipt = buildReceipt(reservation, room.get());
            String receiptText = formatReceipt(receipt);
            String emailBody = buildConfirmationEmail(reservation.getGuestName(), receiptText);
            messageService.sendEmail(reservation.getGuestEmail(), "Reserva confirmada - Pousada Maré Alta", emailBody);
        }

        return reservation;
    }

    public Reservation checkIn(long id) {
        Optional<Reservation> opt = getReservation(id);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Reservation not found");
        }

        Reservation reservation = opt.get();

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("Cannot check-in a non-confirmed reservation");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservation;
    }

    public Receipt checkOut(long id) {
        Optional<Reservation> opt = getReservation(id);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Reservation not found");
        }

        Reservation reservation = opt.get();

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("Cannot check-out without prior check-in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);

        Optional<Room> room = roomService.getRoomByNumber(reservation.getRoomNumber());
        if (room.isEmpty()) {
            throw new IllegalArgumentException("Room not found");
        }

        return buildReceipt(reservation, room.get());
    }

    public Reservation cancelReservation(long id) {
        Optional<Reservation> opt = getReservation(id);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Reservation not found");
        }

        Reservation reservation = opt.get();

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
            reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot cancel a reservation at this status");
        }

        BigDecimal refund = BigDecimal.ZERO;
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            if (reservation.getRatePlan() == RatePlan.STANDARD) {
                refund = reservation.getAmountPaid();
            }
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(MoneyUtil.round(refund));

        return reservation;
    }

    public Receipt getReceipt(long id) {
        Optional<Reservation> opt = getReservation(id);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Reservation not found");
        }

        Reservation reservation = opt.get();

        if (reservation.getStatus() != ReservationStatus.CONFIRMED &&
            reservation.getStatus() != ReservationStatus.CHECKED_IN &&
            reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            throw new IllegalArgumentException("Receipt only available for paid reservations");
        }

        Optional<Room> room = roomService.getRoomByNumber(reservation.getRoomNumber());
        if (room.isEmpty()) {
            throw new IllegalArgumentException("Room not found");
        }

        return buildReceipt(reservation, room.get());
    }

    public List<Reservation> getReservationsByRoomAndDate(int roomNumber, LocalDate date) {
        List<Reservation> result = new ArrayList<>();
        for (Reservation r : reservations) {
            if (r.getRoomNumber() == roomNumber &&
                !r.getCheckIn().isAfter(date) &&
                r.getCheckOut().isAfter(date) &&
                r.getStatus() != ReservationStatus.CANCELLED) {
                result.add(r);
            }
        }
        return result;
    }

    public List<Reservation> getConfirmedReservationsByRoomAndDate(int roomNumber, LocalDate date) {
        List<Reservation> result = new ArrayList<>();
        for (Reservation r : reservations) {
            if (r.getRoomNumber() == roomNumber &&
                !r.getCheckIn().isAfter(date) &&
                r.getCheckOut().isAfter(date) &&
                (r.getStatus() == ReservationStatus.CONFIRMED ||
                 r.getStatus() == ReservationStatus.CHECKED_IN ||
                 r.getStatus() == ReservationStatus.CHECKED_OUT)) {
                result.add(r);
            }
        }
        return result;
    }

    public List<Reservation> getReservationsByMonthStarting(int year, int month) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.plusMonths(1);

        List<Reservation> result = new ArrayList<>();
        for (Reservation r : reservations) {
            if (!r.getCheckIn().isBefore(monthStart) && r.getCheckIn().isBefore(monthEnd) &&
                (r.getStatus() == ReservationStatus.CONFIRMED ||
                 r.getStatus() == ReservationStatus.CHECKED_IN ||
                 r.getStatus() == ReservationStatus.CHECKED_OUT ||
                 r.getStatus() == ReservationStatus.CANCELLED && r.getRefund() != null)) {
                result.add(r);
            }
        }
        return result;
    }

    private boolean isRoomUnavailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            if (!getReservationsByRoomAndDate(roomNumber, current).isEmpty()) {
                return true;
            }
            current = current.plusDays(1);
        }
        return false;
    }

    private Receipt buildReceipt(Reservation reservation, Room room) {
        return new Receipt(
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

    private String formatReceipt(Receipt receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(receipt.getReservationId()).append("\n");
        sb.append("Hóspede: ").append(receipt.getGuestName()).append(" (CPF ").append(formatCpf(receipt.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(receipt.getRoomNumber()).append(" (").append(receipt.getRoomType()).append(")\n");
        sb.append("Entrada: ").append(formatDate(receipt.getCheckIn())).append(" | ");
        sb.append("Saída: ").append(formatDate(receipt.getCheckOut())).append(" | ");
        sb.append(receipt.getNights()).append(receipt.getNights() == 1 ? " diária\n" : " diárias\n");
        sb.append("Diárias: ").append(formatMoney(receipt.getSubtotal())).append("\n");
        if (receipt.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(receipt.getDiscount())).append("\n");
        }
        sb.append("Total: ").append(formatMoney(receipt.getTotal()));
        return sb.toString();
    }

    private String buildConfirmationEmail(String guestName, String receiptText) {
        return "Olá, " + guestName + "!\n\n" +
               "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
               receiptText + "\n\n" +
               "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
               "Até breve!";
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", ".");
    }

    private String formatCpf(String cpf) {
        if (cpf.length() != 11) return cpf;
        return String.format("%s.%s.%s-%s",
                cpf.substring(0, 3),
                cpf.substring(3, 6),
                cpf.substring(6, 9),
                cpf.substring(9, 11));
    }
}

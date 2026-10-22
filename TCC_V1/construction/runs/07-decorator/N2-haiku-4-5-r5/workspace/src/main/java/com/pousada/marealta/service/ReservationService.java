package com.pousada.marealta.service;

import com.pousada.marealta.domain.RatePlan;
import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.ReservationStatus;
import com.pousada.marealta.domain.Room;
import com.pousada.marealta.repository.MessageRepository;
import com.pousada.marealta.repository.ReservationRepository;
import com.pousada.marealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageService messageService;

    public ReservationService(
            ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            MessageRepository messageRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageService = new MessageService(messageRepository);
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
    ) throws Exception {
        Room room = roomRepository.getRoomByNumber(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("ROOM_NOT_FOUND");
        }

        if (guests < 1 || guests > room.getCapacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        List<Reservation> conflicts = reservationRepository.getReservationsByRoomAndDate(roomNumber, checkIn);
        for (Reservation r : conflicts) {
            if (r.getCheckOut().isAfter(checkIn) && r.getCheckIn().isBefore(checkOut)) {
                throw new IllegalArgumentException("ROOM_UNAVAILABLE");
            }
        }

        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = PriceCalculator.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, ratePlan == RatePlan.NON_REFUNDABLE);
        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);

        long reservationId = reservationRepository.create(
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

        return reservationRepository.getById(reservationId);
    }

    public Reservation getReservation(long id) throws Exception {
        Reservation reservation = reservationRepository.getById(id);
        if (reservation == null) {
            throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
        }
        return reservation;
    }

    public Reservation confirmReservation(long id, BigDecimal amountPaid) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservationRepository.updateAmountPaid(id, amountPaid);
        Reservation updated = getReservation(id);

        Room room = roomRepository.getRoomByNumber(reservation.getRoomNumber());
        sendConfirmationEmail(updated, room.getType().getName());

        return updated;
    }

    public Reservation checkIn(long id) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservationRepository.updateStatus(id, ReservationStatus.CHECKED_IN);
        return getReservation(id);
    }

    public Reservation checkOut(long id) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservationRepository.updateStatus(id, ReservationStatus.CHECKED_OUT);
        return getReservation(id);
    }

    public Reservation cancelReservation(long id) throws Exception {
        Reservation reservation = getReservation(id);

        if (reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        BigDecimal refund = calculateRefund(reservation);
        reservationRepository.updateRefund(id, refund);
        return getReservation(id);
    }

    private BigDecimal calculateRefund(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.PENDING) {
            return BigDecimal.ZERO;
        }

        if (reservation.isRefundable()) {
            return reservation.getAmountPaid();
        }

        return BigDecimal.ZERO;
    }

    private void sendConfirmationEmail(Reservation reservation, String roomType) throws Exception {
        String comprovante = buildComprovante(reservation, roomType);

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.getGuestName(),
            comprovante
        );

        messageService.sendEmail(reservation.getGuestEmail(), subject, body);
    }

    public String buildComprovante(Reservation reservation, String roomType) {
        String nights = reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias";

        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName())
          .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber())
          .append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(formatDate(reservation.getCheckIn()))
          .append(" | Saída: ").append(formatDate(reservation.getCheckOut()))
          .append(" | ").append(nights).append("\n");
        sb.append("Diárias: ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().signum() > 0) {
            sb.append("Desconto: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(reservation.getTotal()));

        return sb.toString();
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatMoney(BigDecimal value) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setGroupingSeparator('.');
        dfs.setDecimalSeparator(',');
        df.setDecimalFormatSymbols(dfs);
        return "R$ " + df.format(value);
    }

    private String formatCpf(String cpf) {
        return cpf.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
    }
}

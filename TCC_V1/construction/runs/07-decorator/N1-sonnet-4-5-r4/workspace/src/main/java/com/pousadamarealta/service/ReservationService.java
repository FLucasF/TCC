package com.pousadamarealta.service;

import com.pousadamarealta.model.*;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MessageService messageService;

    public ReservationService(ReservationRepository reservationRepository,
                            RoomRepository roomRepository,
                            MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.messageService = messageService;
    }

    public Reservation create(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
                .orElseThrow(() -> new RoomNotFoundException());

        if (reservation.getGuests() > room.getCapacity()) {
            throw new CapacityExceededException();
        }

        List<Reservation> conflicts = reservationRepository.findByRoomAndDateRange(
                reservation.getRoomNumber(),
                reservation.getCheckIn(),
                reservation.getCheckOut()
        );

        if (!conflicts.isEmpty()) {
            throw new RoomUnavailableException();
        }

        return reservationRepository.save(reservation);
    }

    public Reservation findById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException());
    }

    public Reservation confirm(Long id) {
        Reservation reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new OperationNotAllowedException();
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
        BigDecimal subtotal = reservation.calculateSubtotal(room.getNightlyRate());
        BigDecimal discount = reservation.calculateDiscount(subtotal);
        BigDecimal total = subtotal.subtract(discount);

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setAmountPaid(total);

        Reservation saved = reservationRepository.save(reservation);

        sendConfirmationEmail(saved, room, subtotal, discount, total);

        return saved;
    }

    public Reservation cancel(Long id) {
        Reservation reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.PENDING &&
            reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        BigDecimal refund = BigDecimal.ZERO;

        if (reservation.getStatus() == ReservationStatus.CONFIRMED &&
            reservation.getRatePlan() == RatePlan.STANDARD) {
            refund = reservation.getAmountPaid();
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setRefund(refund);

        return reservationRepository.save(reservation);
    }

    public Reservation checkIn(Long id) {
        Reservation reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        return reservationRepository.save(reservation);
    }

    public Reservation checkOut(Long id) {
        Reservation reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new OperationNotAllowedException();
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return reservationRepository.save(reservation);
    }

    public ReservationDocument getDocument(Long id) {
        Reservation reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED &&
            reservation.getStatus() != ReservationStatus.CHECKED_IN &&
            reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            return null;
        }

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
        BigDecimal subtotal = reservation.calculateSubtotal(room.getNightlyRate());
        BigDecimal discount = reservation.calculateDiscount(subtotal);
        BigDecimal total = subtotal.subtract(discount);

        return createDocument(reservation, room, subtotal, discount, total);
    }

    private void sendConfirmationEmail(Reservation reservation, Room room,
                                     BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        ReservationDocument doc = createDocument(reservation, room, subtotal, discount, total);
        String receipt = formatReceipt(doc);

        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", reservation.getGuestName(), receipt);

        Message message = new Message(
            MessageChannel.EMAIL,
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );

        messageService.send(message);
    }

    private ReservationDocument createDocument(Reservation reservation, Room room,
                                             BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        ReservationDocument doc = new ReservationDocument();
        doc.setTitle("Comprovante de reserva");
        doc.setReservationId(reservation.getId());
        doc.setGuestName(reservation.getGuestName());
        doc.setGuestCpf(reservation.getGuestCpf());
        doc.setRoomNumber(reservation.getRoomNumber());
        doc.setRoomType(room.getType().name());
        doc.setCheckIn(reservation.getCheckIn());
        doc.setCheckOut(reservation.getCheckOut());
        doc.setNights(reservation.getNights());
        doc.setSubtotal(subtotal);
        doc.setDiscount(discount.compareTo(BigDecimal.ZERO) > 0 ? discount : null);
        doc.setTotal(total);
        return doc;
    }

    private String formatReceipt(ReservationDocument doc) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d\n", doc.getReservationId()));
        sb.append(String.format("Hóspede: %s (CPF %s)\n", doc.getGuestName(), doc.getGuestCpf()));
        sb.append(String.format("Quarto: %s (%s)\n", doc.getRoomNumber(), doc.getRoomType()));
        sb.append(String.format("Entrada: %s | Saída: %s | %d diária%s\n",
                formatDate(doc.getCheckIn()),
                formatDate(doc.getCheckOut()),
                doc.getNights(),
                doc.getNights() == 1 ? "" : "s"));
        sb.append(String.format("Diárias: %s\n", formatMoney(doc.getSubtotal())));

        if (doc.getDiscount() != null) {
            sb.append(String.format("Desconto: %s\n", formatMoney(doc.getDiscount())));
        }

        sb.append(String.format("Total: %s", formatMoney(doc.getTotal())));

        return sb.toString();
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }

    public List<Reservation> findByDate(LocalDate date) {
        return reservationRepository.findByDate(date);
    }

    public List<Reservation> findByMonth(int year, int month) {
        return reservationRepository.findByMonth(year, month);
    }

    public static class RoomNotFoundException extends RuntimeException {}
    public static class ReservationNotFoundException extends RuntimeException {}
    public static class RoomUnavailableException extends RuntimeException {}
    public static class CapacityExceededException extends RuntimeException {}
    public static class OperationNotAllowedException extends RuntimeException {}
}

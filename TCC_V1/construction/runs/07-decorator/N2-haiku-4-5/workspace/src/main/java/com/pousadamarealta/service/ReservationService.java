package com.pousadamarealta.service;

import com.pousadamarealta.db.ReservationRepository;
import com.pousadamarealta.db.RoomRepository;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final PricingService pricingService;
    private final MessageService messageService;

    public ReservationService(ReservationRepository reservationRepository, RoomRepository roomRepository,
                            PricingService pricingService, MessageService messageService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.pricingService = pricingService;
        this.messageService = messageService;
    }

    public Reservation createReservation(Integer roomNumber, String guestName, String guestEmail, String guestCpf,
                                         LocalDate checkIn, LocalDate checkOut, Integer guests, String ratePlan)
            throws Exception {
        validateReservationInput(roomNumber, checkIn, checkOut, guests);

        Room room = roomRepository.findByNumber(roomNumber)
            .orElseThrow(() -> new IllegalArgumentException("ROOM_NOT_FOUND"));

        if (guests > room.capacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        checkRoomAvailability(roomNumber, checkIn, checkOut);

        int nights = pricingService.calculateNights(checkIn, checkOut);
        BigDecimal subtotal = pricingService.calculateSubtotal(room.nightlyRate(), checkIn, checkOut);
        BigDecimal discount = pricingService.calculateDiscount(subtotal, ratePlan);
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setGuestCpf(guestCpf);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(ratePlan);
        reservation.setNights(nights);
        reservation.setSubtotal(subtotal);
        reservation.setDiscount(discount);
        reservation.setTotal(total);
        reservation.setStatus("PENDING");
        reservation.setAmountPaid(BigDecimal.ZERO);

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(Long reservationId, BigDecimal amountPaid) throws Exception {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (!"PENDING".equals(reservation.getStatus())) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus("CONFIRMED");
        reservation.setAmountPaid(amountPaid);
        reservationRepository.update(reservation);

        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
        String receipt = generateReceipt(reservation, room);
        String emailBody = generateConfirmationEmail(reservation, room, receipt);
        messageService.sendMessage(reservation.getGuestEmail(), "Reserva confirmada - Pousada Maré Alta", emailBody);

        return reservation;
    }

    public Reservation checkIn(Long reservationId) throws Exception {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (!"CONFIRMED".equals(reservation.getStatus())) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus("CHECKED_IN");
        reservationRepository.update(reservation);
        return reservation;
    }

    public Reservation checkOut(Long reservationId) throws Exception {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (!"CHECKED_IN".equals(reservation.getStatus())) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        reservation.setStatus("CHECKED_OUT");
        reservationRepository.update(reservation);
        return reservation;
    }

    public Reservation cancelReservation(Long reservationId) throws Exception {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

        if (!"PENDING".equals(reservation.getStatus()) && !"CONFIRMED".equals(reservation.getStatus())) {
            throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
        }

        BigDecimal refund = BigDecimal.ZERO;
        if ("CONFIRMED".equals(reservation.getStatus()) && "STANDARD".equals(reservation.getRatePlan())) {
            refund = reservation.getAmountPaid();
        }

        reservation.setStatus("CANCELLED");
        reservation.setRefund(refund);
        reservationRepository.update(reservation);
        return reservation;
    }

    public String generateReceipt(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(room.type()).append(")\n");
        sb.append("Entrada: ").append(formatDate(reservation.getCheckIn())).append(" | ");
        sb.append("Saída: ").append(formatDate(reservation.getCheckOut())).append(" | ");
        sb.append(reservation.getNights()).append(" ").append(reservation.getNights() == 1 ? "diária" : "diárias").append("\n");
        sb.append("Diárias: ").append(formatMoney(reservation.getSubtotal())).append("\n");
        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }
        sb.append("Total: ").append(formatMoney(reservation.getTotal())).append("\n");
        return sb.toString();
    }

    private String generateConfirmationEmail(Reservation reservation, Room room, String receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(receipt).append("\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }

    private void validateReservationInput(Integer roomNumber, LocalDate checkIn, LocalDate checkOut, Integer guests)
            throws Exception {
        if (roomNumber == null || checkIn == null || checkOut == null || guests == null) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }
        if (guests < 1) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }
    }

    private void checkRoomAvailability(Integer roomNumber, LocalDate checkIn, LocalDate checkOut) throws Exception {
        var conflicting = reservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
        if (!conflicting.isEmpty()) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatMoney(BigDecimal value) {
        return String.format("R$ %s", value.toPlainString().replace(".", ","));
    }

    private String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }
}

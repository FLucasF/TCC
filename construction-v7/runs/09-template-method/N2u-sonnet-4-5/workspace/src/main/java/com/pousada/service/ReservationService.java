package com.pousada.service;

import com.pousada.domain.*;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final NotificationService notificationService;

    public ReservationService(ReservationRepository reservationRepository,
                             RoomRepository roomRepository,
                             NotificationService notificationService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.notificationService = notificationService;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail,
                                        String guestCpf, LocalDate checkIn, LocalDate checkOut,
                                        int guests, RatePlan ratePlan) {

        Room room = roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new IllegalArgumentException("ROOM_NOT_FOUND"));

        if (guests < 1) {
            throw new IllegalArgumentException("INVALID_REQUEST");
        }

        if (guests > room.getCapacity()) {
            throw new IllegalArgumentException("CAPACITY_EXCEEDED");
        }

        if (reservationRepository.hasConflict(roomNumber, checkIn, checkOut, null)) {
            throw new IllegalArgumentException("ROOM_UNAVAILABLE");
        }

        long id = reservationRepository.generateId();
        Reservation reservation = new Reservation(
                id, roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan
        );

        return reservationRepository.save(reservation);
    }

    public Reservation getReservation(long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = getReservation(id);
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        BigDecimal subtotal = reservation.calculateSubtotal(room.getNightlyRate());
        BigDecimal discount = reservation.calculateDiscount(subtotal);
        BigDecimal total = reservation.calculateTotal(subtotal, discount);

        reservation.confirm(total);

        String document = generateDocument(reservation, room, subtotal, discount, total);
        sendConfirmationEmail(reservation, document);

        return reservation;
    }

    public Reservation cancelReservation(long id) {
        Reservation reservation = getReservation(id);
        reservation.cancel();
        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = getReservation(id);
        reservation.checkIn();
        return reservation;
    }

    public Reservation checkOut(long id) {
        Reservation reservation = getReservation(id);
        reservation.checkOut();
        return reservation;
    }

    public String generateDocument(Reservation reservation, Room room,
                                   BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        StringBuilder doc = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        doc.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        doc.append("Hóspede: ").append(reservation.getGuestName())
           .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        doc.append("Quarto: ").append(reservation.getRoomNumber())
           .append(" (").append(room.getType()).append(")\n");
        doc.append("Entrada: ").append(reservation.getCheckIn().format(dateFormatter))
           .append(" | Saída: ").append(reservation.getCheckOut().format(dateFormatter))
           .append(" | ").append(reservation.getNights())
           .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        doc.append("Diárias: ").append(formatMoney(subtotal)).append("\n");

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            doc.append("Desconto: ").append(formatMoney(discount)).append("\n");
        }

        doc.append("Total: ").append(formatMoney(total));

        return doc.toString();
    }

    private void sendConfirmationEmail(Reservation reservation, String document) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format("""
                Olá, %s!

                Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                %s

                A entrada é a partir das 14h, e a saída, até as 12h.
                Até breve!""", reservation.getGuestName(), document);

        notificationService.sendEmail(reservation.getGuestEmail(), subject, body);
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatMoney(BigDecimal amount) {
        return String.format(Locale.forLanguageTag("pt-BR"), "R$ %,.2f", amount);
    }
}

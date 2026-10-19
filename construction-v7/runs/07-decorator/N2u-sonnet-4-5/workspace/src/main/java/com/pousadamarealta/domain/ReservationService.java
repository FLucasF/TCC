package com.pousadamarealta.domain;

import com.pousadamarealta.messaging.MessageService;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                         LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Room room = roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (guests > room.getCapacity()) {
            throw new IllegalArgumentException("Capacity exceeded");
        }

        if (!reservationRepository.isRoomAvailable(roomNumber, checkIn, checkOut, null)) {
            throw new IllegalStateException("Room unavailable");
        }

        long id = reservationRepository.nextId();
        Reservation reservation = new Reservation(id, roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan);

        return reservationRepository.save(reservation);
    }

    public Reservation confirmReservation(long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        reservation.confirm(room.getNightlyRate());

        // Enviar e-mail de confirmação
        sendConfirmationEmail(reservation, room);

        return reservation;
    }

    public Reservation checkIn(long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        reservation.checkIn();
        return reservation;
    }

    public Reservation checkOut(long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        reservation.checkOut();
        return reservation;
    }

    public Reservation cancel(long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        reservation.cancel();
        return reservation;
    }

    public String generateDocument(Reservation reservation, Room room) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        StringBuilder doc = new StringBuilder();
        doc.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        doc.append("Hóspede: ").append(reservation.getGuestName())
                .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        doc.append("Quarto: ").append(reservation.getRoomNumber())
                .append(" (").append(room.getType().name()).append(")\n");
        doc.append("Entrada: ").append(reservation.getCheckIn().format(dateFormatter))
                .append(" | Saída: ").append(reservation.getCheckOut().format(dateFormatter))
                .append(" | ").append(reservation.getNights())
                .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");

        BigDecimal subtotal = reservation.calculateSubtotal(room.getNightlyRate());
        doc.append("Diárias: ").append(currency.format(subtotal)).append("\n");

        BigDecimal discount = reservation.calculateDiscount(subtotal);
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            doc.append("Desconto: ").append(currency.format(discount)).append("\n");
        }

        BigDecimal total = reservation.calculateTotal(room.getNightlyRate());
        doc.append("Total: ").append(currency.format(total));

        return doc.toString();
    }

    private void sendConfirmationEmail(Reservation reservation, Room room) {
        String document = generateDocument(reservation, room);

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format("Olá, %s!\n\n" +
                        "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                        "%s\n\n" +
                        "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                        "Até breve!",
                reservation.getGuestName(), document);

        messageService.send(reservation.getGuestEmail(), subject, body);
    }

    private String formatCpf(String cpf) {
        if (cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }
}

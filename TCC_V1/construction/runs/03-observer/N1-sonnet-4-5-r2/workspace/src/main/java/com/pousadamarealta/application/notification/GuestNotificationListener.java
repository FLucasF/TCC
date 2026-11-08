package com.pousadamarealta.application.notification;

import com.pousadamarealta.domain.event.EventListener;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.domain.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class GuestNotificationListener implements EventListener<ReservationConfirmedEvent> {
    private final NotificationService notificationService;
    private final RoomRepository roomRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale BR_LOCALE = new Locale("pt", "BR");

    public GuestNotificationListener(NotificationService notificationService, RoomRepository roomRepository) {
        this.notificationService = notificationService;
        this.roomRepository = roomRepository;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation reservation = event.reservation();
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildEmailBody(reservation, room);

        notificationService.sendEmail(reservation.getGuestEmail(), subject, body);
    }

    private String buildEmailBody(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(buildReceipt(reservation, room));
        sb.append("\n\nA entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }

    private String buildReceipt(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName())
          .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber())
          .append(" (").append(room.getType()).append(")\n");
        sb.append("Entrada: ").append(reservation.getCheckIn().format(DATE_FORMATTER))
          .append(" | Saída: ").append(reservation.getCheckOut().format(DATE_FORMATTER))
          .append(" | ").append(reservation.getNights()).append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");

        BigDecimal subtotal = reservation.calculateSubtotal(room);
        BigDecimal discount = reservation.calculateDiscount(subtotal);
        BigDecimal total = reservation.calculateTotal(room);

        sb.append("Diárias: ").append(formatMoney(subtotal)).append("\n");

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(discount)).append("\n");
        }

        sb.append("Total: ").append(formatMoney(total));

        return sb.toString();
    }

    private String formatCpf(String cpf) {
        return cpf.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
    }

    private String formatMoney(BigDecimal amount) {
        return String.format(BR_LOCALE, "R$ %,.2f", amount);
    }
}

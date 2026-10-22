package com.pousadamarealta.service.event.listeners;

import com.pousadamarealta.domain.enums.RoomType;
import com.pousadamarealta.domain.events.ReservationConfirmedEvent;
import com.pousadamarealta.domain.events.ReservationEvent;
import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.event.EventListener;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class GuestNotificationListener implements EventListener {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MONEY_FORMATTER;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        MONEY_FORMATTER = new DecimalFormat("#,##0.00", symbols);
    }

    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;

    public GuestNotificationListener(MessageRepository messageRepository, RoomRepository roomRepository) {
        this.messageRepository = messageRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    public void handle(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent) {
            sendConfirmationEmail((ReservationConfirmedEvent) event);
        }
    }

    private void sendConfirmationEmail(ReservationConfirmedEvent event) {
        Reservation reservation = event.getReservation();
        RoomType roomType = roomRepository.findByNumber(reservation.getRoomNumber())
                .orElseThrow()
                .getType();

        String receipt = buildReceipt(reservation, roomType);
        String body = buildConfirmationBody(reservation.getGuestName(), receipt);

        Message message = Message.email(
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );

        messageRepository.save(message);
    }

    private String buildReceipt(Reservation reservation, RoomType roomType) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName())
          .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber())
          .append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(formatDate(reservation.getCheckIn()))
          .append(" | Saída: ").append(formatDate(reservation.getCheckOut()))
          .append(" | ").append(reservation.getNights())
          .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(reservation.getTotal()));

        return sb.toString();
    }

    private String buildConfirmationBody(String guestName, String receipt) {
        return String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            guestName, receipt
        );
    }

    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    private String formatMoney(BigDecimal amount) {
        return "R$ " + MONEY_FORMATTER.format(amount);
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }
}

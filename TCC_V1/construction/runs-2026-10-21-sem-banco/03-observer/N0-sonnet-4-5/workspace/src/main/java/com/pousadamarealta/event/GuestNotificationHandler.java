package com.pousadamarealta.event;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.RoomService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class GuestNotificationHandler implements EventHandler<ReservationConfirmedEvent> {
    private final MessageService messageService;
    private final RoomService roomService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public GuestNotificationHandler(MessageService messageService, RoomService roomService) {
        this.messageService = messageService;
        this.roomService = roomService;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation r = event.getReservation();

        String receipt = buildReceipt(r);
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", r.getGuestName(), receipt);

        messageService.send(MessageChannel.EMAIL, r.getGuestEmail(), subject, body);
    }

    private String buildReceipt(Reservation r) {
        RoomType roomType = roomService.findByNumber(r.getRoomNumber()).orElseThrow().getType();

        StringBuilder receipt = new StringBuilder();
        receipt.append(String.format("Comprovante de reserva nº %d\n", r.getId()));
        receipt.append(String.format("Hóspede: %s (CPF %s)\n", r.getGuestName(), formatCpf(r.getGuestCpf())));
        receipt.append(String.format("Quarto: %d (%s)\n", r.getRoomNumber(), roomType));
        receipt.append(String.format("Entrada: %s | Saída: %s | %d %s\n",
            formatDate(r.getCheckIn()),
            formatDate(r.getCheckOut()),
            r.getNights(),
            r.getNights() == 1 ? "diária" : "diárias"));
        receipt.append(String.format("Diárias: %s\n", formatMoney(r.getSubtotal())));

        if (r.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("Desconto: %s\n", formatMoney(r.getDiscount())));
        }

        receipt.append(String.format("Total: %s", formatMoney(r.getTotal())));

        return receipt.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return String.format("%s.%s.%s-%s",
                cpf.substring(0, 3),
                cpf.substring(3, 6),
                cpf.substring(6, 9),
                cpf.substring(9, 11));
        }
        return cpf;
    }

    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }
}

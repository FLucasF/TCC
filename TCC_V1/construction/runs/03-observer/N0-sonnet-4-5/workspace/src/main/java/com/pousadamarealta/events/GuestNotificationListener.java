package com.pousadamarealta.events;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class GuestNotificationListener implements EventListener<ReservationConfirmedEvent> {
    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale BRAZIL_LOCALE = new Locale("pt", "BR");

    public GuestNotificationListener(MessageRepository messageRepository, RoomRepository roomRepository) {
        this.messageRepository = messageRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation reservation = event.reservation();
        String receipt = buildReceipt(reservation);

        String body = String.format("""
                Olá, %s!

                Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                %s

                A entrada é a partir das 14h, e a saída, até as 12h.
                Até breve!""", reservation.getGuestName(), receipt);

        Message message = new Message(
                "EMAIL",
                reservation.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta",
                body
        );

        messageRepository.save(message);
    }

    private String buildReceipt(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d%n", reservation.getId()));
        sb.append(String.format("Hóspede: %s (CPF %s)%n",
                reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));
        sb.append(String.format("Quarto: %s (%s)%n",
                reservation.getRoomNumber(), room.getType()));
        sb.append(String.format("Entrada: %s | Saída: %s | %s%n",
                reservation.getCheckIn().format(DATE_FORMATTER),
                reservation.getCheckOut().format(DATE_FORMATTER),
                reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias"));
        sb.append(String.format("Diárias: %s%n", formatCurrency(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s%n", formatCurrency(reservation.getDiscount())));
        }

        sb.append(String.format("Total: %s", formatCurrency(reservation.getTotal())));

        return sb.toString();
    }

    private String formatCpf(String cpf) {
        return cpf.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(BRAZIL_LOCALE);
        return formatter.format(amount);
    }
}

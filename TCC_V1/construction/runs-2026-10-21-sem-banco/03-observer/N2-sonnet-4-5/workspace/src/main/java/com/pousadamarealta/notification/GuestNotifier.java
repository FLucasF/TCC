package com.pousadamarealta.notification;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class GuestNotifier implements ReservationListener {
    private final MessageSender messageSender;

    public GuestNotifier(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public void onEvent(ReservationEvent event) {
        if (event instanceof ReservationEvent.Confirmed confirmed) {
            sendConfirmationEmail(confirmed.reservation());
        }
    }

    private void sendConfirmationEmail(Reservation reservation) {
        String receipt = buildReceipt(reservation);
        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", reservation.guestName(), receipt);

        messageSender.send(
            MessageChannel.EMAIL,
            reservation.guestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );
    }

    private String buildReceipt(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d\n", reservation.id()));
        sb.append(String.format("Hóspede: %s (CPF %s)\n",
            reservation.guestName(), formatCpf(reservation.guestCpf())));
        sb.append(String.format("Quarto: %d (%s)\n",
            reservation.roomNumber(), reservation.roomType()));
        sb.append(String.format("Entrada: %s | Saída: %s | %s\n",
            formatDate(reservation.checkIn()),
            formatDate(reservation.checkOut()),
            formatNights(reservation.nights())));
        sb.append(String.format("Diárias: %s\n", formatMoney(reservation.subtotal())));
        if (reservation.discount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s\n", formatMoney(reservation.discount())));
        }
        sb.append(String.format("Total: %s", formatMoney(reservation.total())));
        return sb.toString();
    }

    private String formatCpf(String cpf) {
        return cpf.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
    }

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String formatNights(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    private String formatMoney(BigDecimal amount) {
        return String.format(Locale.of("pt", "BR"), "R$ %,.2f", amount);
    }
}

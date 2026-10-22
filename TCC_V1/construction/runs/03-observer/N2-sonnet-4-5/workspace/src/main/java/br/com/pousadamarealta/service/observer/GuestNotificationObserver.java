package br.com.pousadamarealta.service.observer;

import br.com.pousadamarealta.domain.Message;
import br.com.pousadamarealta.domain.Reservation;
import br.com.pousadamarealta.repository.MessageRepository;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class GuestNotificationObserver implements ReservationObserver {
    private final MessageRepository messageRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale PT_BR = new Locale.Builder().setLanguage("pt").setRegion("BR").build();

    public GuestNotificationObserver(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
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

    @Override
    public void onReservationCancelled(Reservation reservation, boolean wasConfirmed) {
    }

    private String buildReceipt(Reservation reservation) {
        StringBuilder receipt = new StringBuilder();
        receipt.append(String.format("Comprovante de reserva nº %d%n", reservation.getId()));
        receipt.append(String.format("Hóspede: %s (CPF %s)%n",
            reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));
        receipt.append(String.format("Quarto: %s (%s)%n",
            reservation.getRoomNumber(), reservation.getRoomType()));
        receipt.append(String.format("Entrada: %s | Saída: %s | %s%n",
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER),
            formatNights(reservation.getNights())));
        receipt.append(String.format("Diárias: %s%n", formatMoney(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("Desconto: %s%n", formatMoney(reservation.getDiscount())));
        }

        receipt.append(String.format("Total: %s", formatMoney(reservation.getTotal())));

        return receipt.toString();
    }

    private String formatCpf(String cpf) {
        return cpf.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
    }

    private String formatNights(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    private String formatMoney(BigDecimal amount) {
        return String.format(PT_BR, "R$ %,.2f", amount);
    }
}

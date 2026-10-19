package com.pousadamarealta.domain.listener;

import com.pousadamarealta.domain.event.Event;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.service.MessageService;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

public class GuestNotificationListener implements Consumer<Event> {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private final MessageService messageService;

    public GuestNotificationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void accept(Event event) {
        if (event instanceof ReservationConfirmedEvent confirmedEvent) {
            sendConfirmationEmail(confirmedEvent.reservation());
        }
    }

    private void sendConfirmationEmail(Reservation reservation) {
        String voucher = generateVoucher(reservation);
        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", reservation.getGuestName(), voucher);

        messageService.sendEmail(
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );
    }

    private String generateVoucher(Reservation reservation) {
        StringBuilder voucher = new StringBuilder();
        voucher.append(String.format("Comprovante de reserva nº %d%n", reservation.getId()));
        voucher.append(String.format("Hóspede: %s (CPF %s)%n",
            reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));
        voucher.append(String.format("Quarto: %s (%s)%n",
            reservation.getRoomNumber(), getRoomTypeName(reservation.getRoomNumber())));
        voucher.append(String.format("Entrada: %s | Saída: %s | %s%n",
            formatDate(reservation.getCheckIn()),
            formatDate(reservation.getCheckOut()),
            formatNights(reservation.getNights())));
        voucher.append(String.format("Diárias: %s%n", formatMoney(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            voucher.append(String.format("Desconto: %s%n", formatMoney(reservation.getDiscount())));
        }

        voucher.append(String.format("Total: %s", formatMoney(reservation.getTotal())));

        return voucher.toString();
    }

    private String getRoomTypeName(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "";
        };
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    private String formatNights(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    private String formatMoney(BigDecimal amount) {
        return CURRENCY_FORMAT.format(amount);
    }
}

package com.pousadamarealta.services;

import com.pousadamarealta.models.Reservation;
import com.pousadamarealta.repositories.MessageRepository;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class NotificationService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter CURRENCY_FORMAT = DateTimeFormatter.ofPattern("R$ #,##0.00");
    private final MessageRepository messageRepository;

    public NotificationService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void notifyReservationConfirmed(Reservation reservation) throws Exception {
        notifyGuest(reservation);
        notifyGovernance(reservation, "confirmed");
        notifyFinance(reservation, "payment_received");
    }

    public void notifyReservationCancelled(Reservation reservation) throws Exception {
        if (reservation.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            notifyGovernance(reservation, "cancelled");
            if (reservation.getRefund() != null && reservation.getRefund().compareTo(BigDecimal.ZERO) > 0) {
                notifyFinance(reservation, "refund");
            }
        }
    }

    private void notifyGuest(Reservation reservation) throws Exception {
        String receipt = generateReceipt(reservation);
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.getGuestName(),
            receipt
        );
        messageRepository.save("EMAIL", reservation.getGuestEmail(), subject, body);
    }

    private void notifyGovernance(Reservation reservation, String type) throws Exception {
        String subject;
        String body;

        if ("confirmed".equals(type)) {
            subject = "Reserva confirmada - quarto " + reservation.getRoomNumber();
            body = String.format(
                "Quarto: %d\nEntrada: %s\nSaída: %s\nHóspedes: %d",
                reservation.getRoomNumber(),
                reservation.getCheckIn().format(DATE_FORMAT),
                reservation.getCheckOut().format(DATE_FORMAT),
                reservation.getGuests()
            );
        } else if ("cancelled".equals(type)) {
            subject = "Reserva cancelada - quarto " + reservation.getRoomNumber();
            body = String.format(
                "Quarto: %d\nEntrada: %s\nSaída: %s",
                reservation.getRoomNumber(),
                reservation.getCheckIn().format(DATE_FORMAT),
                reservation.getCheckOut().format(DATE_FORMAT)
            );
        } else {
            return;
        }

        messageRepository.save("EMAIL", "governanca@pousadamarealta.com.br", subject, body);
    }

    private void notifyFinance(Reservation reservation, String type) throws Exception {
        String subject;
        String body;

        if ("payment_received".equals(type)) {
            subject = "Pagamento recebido - reserva " + reservation.getId();
            body = String.format(
                "Valor: %s",
                formatCurrency(reservation.getAmountPaid())
            );
        } else if ("refund".equals(type)) {
            subject = "Reembolso - reserva " + reservation.getId();
            body = String.format(
                "Valor: %s",
                formatCurrency(reservation.getRefund())
            );
        } else {
            return;
        }

        messageRepository.save("EMAIL", "financeiro@pousadamarealta.com.br", subject, body);
    }

    public String generateReceipt(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append(String.format("Hóspede: %s (CPF %s)\n",
            reservation.getGuestName(),
            formatCpf(reservation.getGuestCpf())));
        sb.append(String.format("Quarto: %d (%s)\n",
            reservation.getRoomNumber(),
            reservation.getRoomNumber() < 200 ? "STANDARD" :
            reservation.getRoomNumber() < 300 ? "DELUXE" : "SUITE"));
        sb.append(String.format("Entrada: %s | Saída: %s | %d %s\n",
            reservation.getCheckIn().format(DATE_FORMAT),
            reservation.getCheckOut().format(DATE_FORMAT),
            reservation.getNights(),
            reservation.getNights() == 1 ? "diária" : "diárias"));
        sb.append(String.format("Diárias: %s\n", formatCurrency(reservation.getSubtotal())));
        if (reservation.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s\n", formatCurrency(reservation.getDiscount())));
        }
        sb.append(String.format("Total: %s", formatCurrency(reservation.getTotal())));
        return sb.toString();
    }

    private String formatCurrency(BigDecimal amount) {
        String formatted = String.format("%.2f", amount);
        String[] parts = formatted.split("\\.");
        String intPart = parts[0];
        String decPart = parts[1];

        StringBuilder result = new StringBuilder("R$ ");
        int len = intPart.length();
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 3 == 0) {
                result.append(".");
            }
            result.append(intPart.charAt(i));
        }
        result.append(",").append(decPart);
        return result.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        }
        return cpf;
    }
}

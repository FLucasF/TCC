package com.pousada.services;

import com.pousada.models.Receipt;
import com.pousada.models.Reservation;
import com.pousada.models.Room;
import com.pousada.repositories.MessageRepository;
import com.pousada.repositories.RoomRepository;
import com.pousada.utils.DateUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class ReceiptService {
    private static final DecimalFormat MONEY_FORMAT = createMoneyFormat();

    private static DecimalFormat createMoneyFormat() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        return format;
    }

    private static String formatMoney(BigDecimal value) {
        return "R$ " + MONEY_FORMAT.format(value);
    }

    public static Receipt generateReceipt(Reservation reservation) {
        Room room = RoomRepository.findByNumber(reservation.getRoomNumber());

        return new Receipt(
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            room.getType(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }

    public static String formatReceipt(Receipt receipt) {
        StringBuilder sb = new StringBuilder();

        sb.append("Comprovante de reserva nº ").append(receipt.getReservationId()).append("\n");
        sb.append("Hóspede: ").append(receipt.getGuestName())
            .append(" (CPF ").append(receipt.getGuestCpf()).append(")\n");
        sb.append("Quarto: ").append(receipt.getRoomNumber())
            .append(" (").append(receipt.getRoomType()).append(")\n");

        sb.append("Entrada: ").append(DateUtils.formatDate(receipt.getCheckIn()))
            .append(" | Saída: ").append(DateUtils.formatDate(receipt.getCheckOut()))
            .append(" | ");

        if (receipt.getNights() == 1) {
            sb.append("1 diária");
        } else {
            sb.append(receipt.getNights()).append(" diárias");
        }
        sb.append("\n");

        sb.append("Diárias: ").append(formatMoney(receipt.getSubtotal())).append("\n");

        if (receipt.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(receipt.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(receipt.getTotal()));

        return sb.toString();
    }

    public static void sendConfirmationEmail(Reservation reservation) {
        Receipt receipt = generateReceipt(reservation);
        String receiptText = formatReceipt(receipt);

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = "Olá, " + reservation.getGuestName() + "!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            receiptText + "\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!";

        MessageRepository.create("EMAIL", reservation.getGuestEmail(), subject, body);
    }
}

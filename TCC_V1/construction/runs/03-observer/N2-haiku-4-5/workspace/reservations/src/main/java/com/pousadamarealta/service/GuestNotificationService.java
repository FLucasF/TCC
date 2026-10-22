package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.database.RoomDAO;
import com.pousadamarealta.domain.Room;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

public class GuestNotificationService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MONEY_FORMATTER = new DecimalFormat("0.00");

    public static void sendConfirmationEmail(Reservation reservation) throws Exception {
        String receipt = generateReceipt(reservation);
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = generateBody(reservation, receipt);
        MessageService.getInstance().sendMessage(MessageChannel.EMAIL, reservation.getGuestEmail(), subject, body);
    }

    public static String generateReceipt(Reservation reservation) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        sb.append(String.format("Hóspede: %s (CPF %s)\n", reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));

        Room room = RoomDAO.getInstance().getRoomByNumber(reservation.getRoomNumber());
        String roomType = room != null ? room.getTypeCode() : getRoomTypeByNumber(reservation.getRoomNumber());
        sb.append(String.format("Quarto: %d (%s)\n", reservation.getRoomNumber(), roomType));

        String checkIn = reservation.getCheckIn().format(DATE_FORMATTER);
        String checkOut = reservation.getCheckOut().format(DATE_FORMATTER);
        String nightsLabel = reservation.getNights() == 1 ? "diária" : "diárias";
        sb.append(String.format("Entrada: %s | Saída: %s | %d %s\n", checkIn, checkOut, reservation.getNights(), nightsLabel));

        sb.append(String.format("Diárias: %s\n", formatMoney(reservation.getSubtotal())));
        if (reservation.getDiscount().toDouble() > 0) {
            sb.append(String.format("Desconto: %s\n", formatMoney(reservation.getDiscount())));
        }
        sb.append(String.format("Total: %s", formatMoney(reservation.getTotal())));
        return sb.toString();
    }

    private static String generateBody(Reservation reservation, String receipt) {
        return String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.getGuestName(),
            receipt
        );
    }

    private static String formatMoney(com.pousadamarealta.domain.Money money) {
        return "R$ " + MONEY_FORMATTER.format(money.toDouble()).replace(',', '.');
    }

    private static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return String.format("%s.%s.%s-%s",
                cpf.substring(0, 3),
                cpf.substring(3, 6),
                cpf.substring(6, 9),
                cpf.substring(9, 11));
    }

    private static String getRoomTypeByNumber(int roomNumber) {
        if (roomNumber < 200) {
            return "STANDARD";
        } else if (roomNumber < 300) {
            return "DELUXE";
        } else {
            return "SUITE";
        }
    }
}

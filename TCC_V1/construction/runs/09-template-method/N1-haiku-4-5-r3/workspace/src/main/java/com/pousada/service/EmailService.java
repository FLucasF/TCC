package com.pousada.service;

import com.pousada.model.Message;
import com.pousada.model.Reservation;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.model.Room;
import java.time.format.DateTimeFormatter;

public class EmailService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String LODGE_NAME = "Pousada Maré Alta";

    public static void sendConfirmationEmail(Reservation reservation) {
        String voucherContent = generateVoucher(reservation);
        String emailBody = generateConfirmationEmailBody(reservation, voucherContent);

        Message message = new Message(
            "EMAIL",
            reservation.getGuestEmail(),
            "Reserva confirmada - " + LODGE_NAME,
            emailBody
        );

        MessageRepository.save(message);
    }

    public static String generateVoucher(Reservation reservation) {
        Room room = RoomRepository.findByNumber(reservation.getRoomNumber());
        String roomType = room.getType().getName();

        String checkInStr = reservation.getCheckIn().format(DATE_FORMATTER);
        String checkOutStr = reservation.getCheckOut().format(DATE_FORMATTER);
        String nightsText = reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias";

        StringBuilder voucher = new StringBuilder();
        voucher.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        voucher.append("Hóspede: ").append(reservation.getGuestName())
            .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        voucher.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(roomType).append(")\n");
        voucher.append("Entrada: ").append(checkInStr).append(" | Saída: ").append(checkOutStr)
            .append(" | ").append(nightsText).append("\n");
        voucher.append("Diárias: R$ ").append(formatCurrency(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount() != null && reservation.getDiscount().signum() > 0) {
            voucher.append("Desconto: R$ ").append(formatCurrency(reservation.getDiscount())).append("\n");
        }

        voucher.append("Total: R$ ").append(formatCurrency(reservation.getTotal()));

        return voucher.toString();
    }

    private static String generateConfirmationEmailBody(Reservation reservation, String voucherContent) {
        return "Olá, " + reservation.getGuestName() + "!\n\n" +
               "Sua reserva na " + LODGE_NAME + " está confirmada. Estes são os dados dela:\n\n" +
               voucherContent + "\n\n" +
               "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
               "Até breve!";
    }

    private static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    private static String formatCurrency(java.math.BigDecimal value) {
        return String.format("%.2f", value);
    }
}

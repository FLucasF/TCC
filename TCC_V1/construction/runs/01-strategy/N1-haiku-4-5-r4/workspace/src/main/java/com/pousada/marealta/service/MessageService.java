package com.pousada.marealta.service;

import com.pousada.marealta.model.Message;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.persistence.MessageDao;
import com.pousada.marealta.persistence.RoomDao;

import java.sql.SQLException;

public class MessageService {
    private final MessageDao messageDao;
    private final RoomDao roomDao;

    public MessageService(MessageDao messageDao, RoomDao roomDao) {
        this.messageDao = messageDao;
        this.roomDao = roomDao;
    }

    public void sendConfirmationEmail(Reservation reservation) throws SQLException {
        String guestName = reservation.getGuestName();
        String roomType = roomDao.findByNumber(reservation.getRoomNumber()).getType().name();

        String subject = "Reserva confirmada - Pousada Maré Alta";

        String receipt = formatReceipt(reservation, roomType);

        String body = String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            guestName,
            receipt
        );

        Message message = new Message("EMAIL", reservation.getGuestEmail(), subject, body);
        messageDao.save(message);
    }

    private String formatReceipt(Reservation reservation, String roomType) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        sb.append(String.format("Hóspede: %s (CPF %s)\n", reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));
        sb.append(String.format("Quarto: %d (%s)\n", reservation.getRoomNumber(), roomType));

        String nightsLabel = reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias";
        sb.append(String.format("Entrada: %s | Saída: %s | %s\n",
            formatDate(reservation.getCheckIn()),
            formatDate(reservation.getCheckOut()),
            nightsLabel));

        sb.append(String.format("Diárias: R$ %s\n", formatMoney(reservation.getSubtotal())));

        if (reservation.getDiscount().signum() > 0) {
            sb.append(String.format("Desconto: R$ %s\n", formatMoney(reservation.getDiscount())));
        }

        sb.append(String.format("Total: R$ %s", formatMoney(reservation.getTotal())));

        return sb.toString();
    }

    public String getReceiptAsString(Reservation reservation, String roomType) {
        return formatReceipt(reservation, roomType);
    }

    private String formatDate(java.time.LocalDate date) {
        return String.format("%02d/%02d/%d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatMoney(java.math.BigDecimal value) {
        return String.format("%.2f", value).replace(".", ",");
    }

    private String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return String.format("%s.%s.%s-%s",
            cpf.substring(0, 3),
            cpf.substring(3, 6),
            cpf.substring(6, 9),
            cpf.substring(9));
    }
}

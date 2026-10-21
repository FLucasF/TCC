package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.util.Money;
import java.time.format.DateTimeFormatter;

public class DocumentService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ReservationService reservationService;

    public DocumentService(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public String generateReceipt(Reservation reservation) {
        Room room = reservationService.getRoom(reservation.getRoomNumber());

        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(room.getType().getName()).append(")\n");
        sb.append("Entrada: ").append(reservation.getCheckIn().format(DATE_FORMAT));
        sb.append(" | Saída: ").append(reservation.getCheckOut().format(DATE_FORMAT));
        String nightsLabel = reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias";
        sb.append(" | ").append(nightsLabel).append("\n");
        sb.append("Diárias: ").append(Money.formatForReceipt(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount() > 0) {
            sb.append("Desconto: ").append(Money.formatForReceipt(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(Money.formatForReceipt(reservation.getTotal())).append("\n");

        return sb.toString();
    }

    public String generateConfirmationEmail(Reservation reservation) {
        String receipt = generateReceipt(reservation);

        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(receipt).append("\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!\n");

        return sb.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }
}

package com.pousada.document;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.util.MoneyRounder;
import java.time.format.DateTimeFormatter;

public class CnpjInvoiceRenderer implements DocumentRenderer {
    private final Room room;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final double ISS_RATE = 0.05;

    public CnpjInvoiceRenderer(Room room) {
        this.room = room;
    }

    @Override
    public DocumentContent render(Reservation reservation) {
        String checkInFormatted = reservation.getCheckIn().format(DATE_FORMAT);
        String checkOutFormatted = reservation.getCheckOut().format(DATE_FORMAT);

        String nightsLabel = reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias";

        double iss = MoneyRounder.round(reservation.getTotal() * ISS_RATE);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Fatura de hospedagem nº %d\n", reservation.getId()));
        sb.append(String.format("Empresa: %s (CNPJ %s)\n", reservation.getCompanyName(),
                reservation.getCompanyCnpj()));
        sb.append(String.format("Hóspede: %s\n", reservation.getGuestName()));
        sb.append(String.format("Quarto: %d (%s)\n", reservation.getRoomNumber(), room.type.name));
        sb.append(String.format("Entrada: %s | Saída: %s | %s\n", checkInFormatted, checkOutFormatted,
                nightsLabel));
        sb.append(String.format("Diárias: R$ %.2f\n", reservation.getSubtotal()));

        if (reservation.getDiscount() > 0) {
            sb.append(String.format("Desconto: R$ %.2f\n", reservation.getDiscount()));
        }

        sb.append(String.format("Total: R$ %.2f\n", reservation.getTotal()));
        sb.append(String.format("ISS retido (5%%): R$ %.2f", iss));

        String text = sb.toString();

        return new DocumentContent(
                "Fatura de hospedagem",
                reservation.getId(),
                reservation.getGuestName(),
                null,
                reservation.getCompanyCnpj(),
                reservation.getCompanyName(),
                null,
                null,
                reservation.getRoomNumber(),
                room.type.name,
                checkInFormatted,
                checkOutFormatted,
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal(),
                text);
    }
}

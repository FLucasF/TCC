package com.pousada.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CpfIdentification(
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestCpf") String guestCpf
) implements GuestIdentification {

    @Override
    public String getGuestName() {
        return guestName;
    }

    @Override
    public String formatForDocument() {
        return guestName + " (CPF " + guestCpf + ")";
    }

    @Override
    public String generateDocument(Document.DocumentData data) {
        StringBuilder doc = new StringBuilder();
        doc.append("Comprovante de reserva nº ").append(data.reservationId()).append("\n");
        doc.append("Hóspede: ").append(formatForDocument()).append("\n");
        doc.append("Quarto: ").append(data.roomNumber()).append(" (").append(data.roomType()).append(")\n");
        doc.append("Entrada: ").append(data.checkIn()).append(" | Saída: ").append(data.checkOut());
        doc.append(" | ").append(data.nights()).append(data.nights() == 1 ? " diária" : " diárias").append("\n");
        doc.append("Diárias: R$ ").append(formatMoney(data.subtotal())).append("\n");

        if (data.discount() != null && data.discount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            doc.append("Desconto: R$ ").append(formatMoney(data.discount())).append("\n");
        }

        doc.append("Total: R$ ").append(formatMoney(data.total()));

        return doc.toString();
    }

    private String formatMoney(java.math.BigDecimal value) {
        return String.format("%,.2f", value).replace(',', 'X').replace('.', ',').replace('X', '.');
    }
}

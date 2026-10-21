package com.pousada.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CnpjIdentification(
    @JsonProperty("guestName") String guestName,
    @JsonProperty("companyName") String companyName,
    @JsonProperty("companyCnpj") String companyCnpj
) implements GuestIdentification {

    @Override
    public String getGuestName() {
        return guestName;
    }

    @Override
    public String formatForDocument() {
        return companyName + " (CNPJ " + companyCnpj + ")";
    }

    @Override
    public String generateDocument(Document.DocumentData data) {
        StringBuilder doc = new StringBuilder();
        doc.append("Fatura de hospedagem nº ").append(data.reservationId()).append("\n");
        doc.append("Empresa: ").append(formatForDocument()).append("\n");
        doc.append("Hóspede: ").append(guestName).append("\n");
        doc.append("Quarto: ").append(data.roomNumber()).append(" (").append(data.roomType()).append(")\n");
        doc.append("Entrada: ").append(data.checkIn()).append(" | Saída: ").append(data.checkOut());
        doc.append(" | ").append(data.nights()).append(data.nights() == 1 ? " diária" : " diárias").append("\n");
        doc.append("Diárias: R$ ").append(formatMoney(data.subtotal())).append("\n");

        if (data.discount() != null && data.discount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            doc.append("Desconto: R$ ").append(formatMoney(data.discount())).append("\n");
        }

        doc.append("Total: R$ ").append(formatMoney(data.total())).append("\n");

        java.math.BigDecimal iss = data.total().multiply(new java.math.BigDecimal("0.05"))
            .setScale(2, java.math.RoundingMode.HALF_EVEN);
        doc.append("ISS retido (5%): R$ ").append(formatMoney(iss));

        return doc.toString();
    }

    private String formatMoney(java.math.BigDecimal value) {
        return String.format("%,.2f", value).replace(',', 'X').replace('.', ',').replace('X', '.');
    }
}

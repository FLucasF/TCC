package com.pousada.mare_alta.document;

import com.pousada.mare_alta.model.Document;
import com.pousada.mare_alta.util.NumberFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

public class CnpjDocumentFormatter implements DocumentFormatter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    @Override
    public Document format(DocumentData data) {
        String nightsText = data.getNights() == 1 ? "1 diária" : data.getNights() + " diárias";
        BigDecimal iss = calculateIss(data.getTotal());

        StringBuilder text = new StringBuilder();
        text.append("Fatura de hospedagem nº ").append(data.getReservationId()).append("\n");
        text.append("Empresa: ").append(data.getCompanyName()).append(" (CNPJ ").append(formatCnpj(data.getCompanyCnpj())).append(")\n");
        text.append("Hóspede: ").append(data.getGuestName()).append("\n");
        text.append("Quarto: ").append(data.getRoomNumber()).append(" (").append(data.getRoomType()).append(")\n");
        text.append("Entrada: ").append(data.getCheckIn().format(DATE_FORMATTER));
        text.append(" | Saída: ").append(data.getCheckOut().format(DATE_FORMATTER));
        text.append(" | ").append(nightsText).append("\n");
        text.append("Diárias: ").append(NumberFormatter.formatCurrency(data.getSubtotal())).append("\n");

        if (data.getDiscount().signum() > 0) {
            text.append("Desconto: ").append(NumberFormatter.formatCurrency(data.getDiscount())).append("\n");
        }

        text.append("Total: ").append(NumberFormatter.formatCurrency(data.getTotal())).append("\n");
        text.append("ISS retido (5%): ").append(NumberFormatter.formatCurrency(iss)).append("\n");

        return new Document(
                "Fatura de hospedagem",
                data.getReservationId(), data.getGuestName(),
                null, data.getCompanyCnpj(), data.getCompanyName(), null, null,
                data.getRoomNumber(), data.getRoomType(),
                data.getCheckIn(), data.getCheckOut(), data.getNights(),
                data.getSubtotal(), data.getDiscount(), data.getTotal(),
                text.toString().trim());
    }

    private BigDecimal calculateIss(BigDecimal total) {
        return total.multiply(ISS_RATE).setScale(2, RoundingMode.HALF_EVEN);
    }

    private String formatCnpj(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) {
            return cnpj;
        }
        return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." + cnpj.substring(5, 8) + "/" + cnpj.substring(8, 12) + "-" + cnpj.substring(12);
    }
}

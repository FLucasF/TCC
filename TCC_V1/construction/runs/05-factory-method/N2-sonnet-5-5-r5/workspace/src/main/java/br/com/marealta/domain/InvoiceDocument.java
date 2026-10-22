package br.com.marealta.domain;

import java.math.BigDecimal;
import java.util.List;

/** Fatura de hospedagem, para empresa com CNPJ. */
final class InvoiceDocument extends PortugueseDocument {
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    private final String cnpj;
    private final String companyName;

    InvoiceDocument(Reservation reservation, String cnpj, String companyName) {
        super(reservation);
        this.cnpj = cnpj;
        this.companyName = companyName;
    }

    @Override
    public String title() {
        return "Fatura de hospedagem";
    }

    @Override
    protected List<String> guestLines() {
        return List.of("Empresa: " + companyName + " (CNPJ " + cnpj + ")",
                "Hóspede: " + reservation.guestName());
    }

    @Override
    protected List<String> closingLines() {
        BigDecimal iss = Money.round(reservation.total().multiply(ISS_RATE));
        return List.of("ISS retido (5%): " + Money.brl(iss));
    }
}

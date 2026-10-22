package br.com.marealta;

import java.util.List;

public class InvoiceFormat extends PortugueseDocumentFormat {
    private static final int ISS_PERCENT = 5;

    public String title() { return "Fatura de hospedagem"; }

    protected List<String> partyLines(Reservation r) {
        return List.of(
                "Empresa: " + r.identity().get(CnpjIdentity.NAME) + " (CNPJ " + r.identity().get(CnpjIdentity.CNPJ) + ")",
                "Hóspede: " + r.guestName());
    }

    protected List<String> closingLines(Reservation r) {
        return List.of("ISS retido (" + ISS_PERCENT + "%): " + money(Money.percentOf(r.price().total(), ISS_PERCENT)));
    }
}

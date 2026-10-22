package br.com.mare.pousada;

import java.math.BigDecimal;
import java.util.List;

public class InvoiceDocument extends Document {
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    private final CompanyIdentity company;

    public InvoiceDocument(Reservation reservation, CompanyIdentity company) {
        super(reservation);
        this.company = company;
    }

    @Override
    public String title() {
        return "Fatura de hospedagem";
    }

    @Override
    protected DocumentStyle style() {
        return DocumentStyle.PORTUGUESE;
    }

    @Override
    protected List<String> guestLines() {
        return List.of("Empresa: " + company.companyName() + " (CNPJ " + company.cnpj() + ")",
                "Hóspede: " + reservation.guestName());
    }

    @Override
    protected List<String> closingLines() {
        BigDecimal iss = Money.percent(reservation.quote().total(), ISS_RATE);
        return List.of("ISS retido (5%): " + style().money(iss));
    }
}

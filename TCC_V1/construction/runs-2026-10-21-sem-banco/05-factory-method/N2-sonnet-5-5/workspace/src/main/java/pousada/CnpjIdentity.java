package pousada;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CnpjIdentity(String cnpj, String companyName) implements GuestIdentity {
    public static final IdentityType TYPE = new IdentityType() {
        public List<String> keys() { return List.of("companyCnpj", "companyName"); }

        public GuestIdentity parse(RequestBody body) {
            return new CnpjIdentity(body.text("companyCnpj"), body.text("companyName"));
        }
    };

    public Map<String, String> fields() {
        return Map.of("companyCnpj", cnpj, "companyName", companyName);
    }

    public ReservationDocument issue(DocumentData data) { return new Fatura(data, this); }

    static final class Fatura extends ReservationDocument {
        private static final BigDecimal ISS_RATE = new BigDecimal("0.05");
        private final CnpjIdentity company;

        Fatura(DocumentData data, CnpjIdentity company) {
            super(data, company);
            this.company = company;
        }

        protected String title() { return "Fatura de hospedagem"; }

        protected DocumentLabels labels() { return DocumentLabels.PORTUGUESE; }

        protected List<String> guestLines() {
            return List.of("Empresa: " + company.companyName() + " (CNPJ " + company.cnpj() + ")",
                    "Hóspede: " + data.guestName());
        }

        protected List<String> closingLines() {
            BigDecimal iss = Money.round(data.quote().total().multiply(ISS_RATE));
            return List.of("ISS retido (5%): " + money(iss));
        }
    }
}

package pousada.domain.identification;

import pousada.domain.Money;
import pousada.domain.document.Document;
import pousada.domain.document.PortugueseLayout;
import pousada.domain.document.Voucher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Empresa com CNPJ: recebe a fatura de hospedagem, com o ISS retido. */
record CnpjIdentification(String cnpj, String companyName) implements Identification {

    static final String TITLE = "Fatura de hospedagem";
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    @Override
    public Map<String, String> fields() {
        return Map.of("companyCnpj", cnpj, "companyName", companyName);
    }

    @Override
    public Document issue(Voucher v) {
        BigDecimal iss = Money.round(v.total().multiply(ISS_RATE));
        String text = PortugueseLayout.render(TITLE, v,
                List.of("Empresa: " + companyName + " (CNPJ " + cnpj + ")", "Hóspede: " + v.guestName()),
                List.of("ISS retido (5%): " + Money.brl(iss)));
        return new Document(TITLE, v, IdentificationKinds.withNulls(this), text);
    }

    static final class Kind implements IdentificationKind {
        @Override
        public List<String> fieldNames() {
            return List.of("companyCnpj", "companyName");
        }

        @Override
        public Identification create(Map<String, String> values) {
            return new CnpjIdentification(values.get("companyCnpj"), values.get("companyName"));
        }
    }
}

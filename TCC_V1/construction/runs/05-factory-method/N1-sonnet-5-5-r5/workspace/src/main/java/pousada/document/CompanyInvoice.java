package pousada.document;

import java.util.List;
import java.util.Map;
import pousada.domain.Money;

/** Fatura de hospedagem, para empresa com CNPJ; termina com o ISS retido. */
final class CompanyInvoice extends DocumentType {
    @Override public String code() { return "CNPJ"; }
    @Override public List<String> fields() { return List.of("companyCnpj", "companyName"); }
    @Override public String title() { return "Fatura de hospedagem"; }
    @Override protected Language language() { return Language.PT_BR; }

    @Override
    protected List<String> identityLines(String guestName, Map<String, String> identity) {
        return List.of(
                "Empresa: " + identity.get("companyName") + " (CNPJ " + identity.get("companyCnpj") + ")",
                "Hóspede: " + guestName);
    }

    @Override
    protected List<String> closingLines(DocumentData data) {
        return List.of("ISS retido (5%): " + language().money(Money.times(data.total(), "0.05")));
    }
}

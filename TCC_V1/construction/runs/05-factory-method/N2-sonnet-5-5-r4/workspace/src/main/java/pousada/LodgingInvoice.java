package pousada;

import java.util.List;
import java.util.Map;

class LodgingInvoice extends PortugueseDocument {
    @Override
    String title() {
        return "Fatura de hospedagem";
    }

    @Override
    protected List<String> identityLines(Reservation r) {
        Map<String, String> id = r.identification().values();
        return List.of("Empresa: " + id.get("companyName") + " (CNPJ " + id.get("companyCnpj") + ")",
                "Hóspede: " + r.guestName());
    }

    @Override
    protected List<String> closingLines(Reservation r) {
        return List.of("ISS retido (5%): " + Money.brl(Money.percent(r.pricing().total(), "0.05")));
    }
}

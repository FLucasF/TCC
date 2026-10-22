package br.com.mare.pousada;

import java.util.List;
import java.util.Map;

public record CompanyIdentity(String cnpj, String companyName) implements GuestIdentity {
    public static final IdentityKind KIND = new IdentityKind("COMPANY", List.of("companyCnpj", "companyName"),
            v -> new CompanyIdentity(v.get("companyCnpj"), v.get("companyName")));

    @Override
    public IdentityKind kind() {
        return KIND;
    }

    @Override
    public Map<String, String> values() {
        return Map.of("companyCnpj", cnpj, "companyName", companyName);
    }

    @Override
    public Document document(Reservation reservation) {
        return new InvoiceDocument(reservation, this);
    }
}

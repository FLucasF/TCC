package br.com.marealta.domain;

public record CnpjIdentity(String cnpj, String companyName) implements GuestIdentity {
    public static final IdentityKind KIND = new IdentityKind() {
        @Override
        public boolean isPresent(IdentityFields f) {
            return f.companyCnpj() != null || f.companyName() != null;
        }

        @Override
        public GuestIdentity create(IdentityFields f) {
            return new CnpjIdentity(IdentityKind.require(f.companyCnpj(), "companyCnpj"),
                    IdentityKind.require(f.companyName(), "companyName"));
        }
    };

    @Override
    public IdentityFields fields() {
        return new IdentityFields(null, cnpj, companyName, null, null);
    }

    @Override
    public ReservationDocument documentFor(Reservation reservation) {
        return new InvoiceDocument(reservation, cnpj, companyName);
    }
}

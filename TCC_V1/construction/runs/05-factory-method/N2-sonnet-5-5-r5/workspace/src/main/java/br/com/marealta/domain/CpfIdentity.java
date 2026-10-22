package br.com.marealta.domain;

public record CpfIdentity(String cpf) implements GuestIdentity {
    public static final IdentityKind KIND = new IdentityKind() {
        @Override
        public boolean isPresent(IdentityFields f) {
            return f.guestCpf() != null;
        }

        @Override
        public GuestIdentity create(IdentityFields f) {
            return new CpfIdentity(IdentityKind.require(f.guestCpf(), "guestCpf"));
        }
    };

    @Override
    public IdentityFields fields() {
        return new IdentityFields(cpf, null, null, null, null);
    }

    @Override
    public ReservationDocument documentFor(Reservation reservation) {
        return new ReceiptDocument(reservation, cpf);
    }
}

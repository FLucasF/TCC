package hotel.reservations;

/** How the guest is identified: individual (CPF), corporate (CNPJ), or foreign (passport). */
public sealed interface GuestIdentity permits IndividualIdentity, CorporateIdentity, ForeignIdentity {
}
